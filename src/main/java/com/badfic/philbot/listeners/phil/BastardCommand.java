package com.badfic.philbot.listeners.phil;

import com.badfic.philbot.config.Constants;
import com.badfic.philbot.config.PhilMarker;
import com.badfic.philbot.data.DiscordUser;
import com.badfic.philbot.data.phil.Rank;
import com.badfic.philbot.repository.DiscordUserRepository;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BastardCommand extends Command implements PhilMarker {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static volatile boolean BOOST_AWAITING = false;
    private static volatile String SWIPER_AWAITING = null;
    private static volatile boolean DID_SOMEONE_SAVE_FROM_SWIPER = false;
    private static volatile boolean AWAITING_RESET_CONFIRMATION = false;
    public static final long NORMAL_MSG_POINTS = 10;
    private static final String NO_SWIPING = "https://cdn.discordapp.com/attachments/707453916882665552/757776008639283321/unknown.png";
    private static final String SWIPER_WON = "https://cdn.discordapp.com/attachments/707453916882665552/757774007314677872/iu.png";
    private static final String BENEVOLENT_GOD = "https://cdn.discordapp.com/attachments/686127721688203305/757429302705913876/when-i-level-up-someone-amp-039-s-account_o_2942005.png";
    private static final String TAXES = "https://cdn.discordapp.com/attachments/707453916882665552/757770782737825933/iu.png";
    private static final String ROBINHOOD = "https://cdn.discordapp.com/attachments/707453916882665552/757686616826314802/iu.png";
    private static final String SWEEPSTAKES = "https://cdn.discordapp.com/attachments/707453916882665552/757687192524161035/iu.png";
    private static final String[] LEADERBOARD_MEDALS = {
            "\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49",
            "\uD83D\uDC40", "\uD83D\uDC40", "\uD83D\uDC40", "\uD83D\uDC40", "\uD83D\uDC40", "\uD83D\uDC40", "\uD83D\uDC40"
    };
    private static final String SLOT_MACHINE = "\uD83C\uDFB0";
    private static final Set<String> SLOTS = new HashSet<>(Arrays.asList(
            "\uD83E\uDD5D", "\uD83C\uDF53", "\uD83C\uDF4B", "\uD83E\uDD6D", "\uD83C\uDF51", "\uD83C\uDF48", "\uD83C\uDF4A", "\uD83C\uDF4D", "\uD83C\uDF50",
            "\uD83C\uDF47"
    ));

    private final DiscordUserRepository discordUserRepository;
    private final String userHelp;
    private final String adminHelp;

    @Resource(name = "philJda")
    @Lazy
    private JDA philJda;

    @Autowired
    public BastardCommand(DiscordUserRepository discordUserRepository) {
        name = "bastard";
        requiredRole = Constants.EIGHTEEN_PLUS;
        userHelp =
                "`!!bastard rank` show your bastard rank\n" +
                "`!!bastard leaderboard` show the bastard leaderboard\n" +
                "`!!bastard up @incogmeato` upvote a user for the bastard games\n" +
                "`!!bastard down @incogmeato` downvote a user for the bastard games\n" +
                "`!!bastard flip` Flip a coin. Heads you win, tails you lose\n" +
                "`!!bastard slots` Play slots. Winners for 2 out of 3 matches or 3 out of 3 matches.\n" +
                "`!!bastard steal @incogmeato` Attempt to steal some points from incogmeato";
        adminHelp = userHelp + "\n\nMODS ONLY COMMANDS:\n" +
                "`!!bastard give 120 @incogmeato` give 120 points to incogmeato\n" +
                "`!!bastard take 120 @incogmeato` remove 120 points from incogmeato\n" +
                "`!!bastard set 120 @incogmeato` set incogmeato to 120 points\n" +
                "`!!bastard reset` reset everyone back to level 0";
        this.discordUserRepository = discordUserRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?", zone = "GMT")
    public void sweepstakes() {
        List<DiscordUser> allUsers = discordUserRepository.findAll();

        long startTime = System.currentTimeMillis();
        Member member = null;
        while (member == null && System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(30)) {
            DiscordUser winningUser = pickRandom(allUsers);

            try {
                Member memberById = philJda.getGuilds().get(0).retrieveMemberById(winningUser.getId()).complete();
                if (memberById != null
                        && hasRole(memberById, Constants.EIGHTEEN_PLUS)
                        && winningUser.getXp() > 0
                        && winningUser.getUpdateTime().isAfter(LocalDateTime.now().minusHours(24))) {
                    member = memberById;
                }
            } catch (Exception ignored) {}
        }

        if (member == null) {
            philJda.getTextChannelsByName("bastard-of-the-week", false)
                    .get(0)
                    .sendMessage(simpleEmbed("Sweepstakes Results", "Unable to choose a winner, nobody wins"))
                    .queue();
            return;
        }

        givePointsToMember(4000, member);

        MessageEmbed message = new EmbedBuilder()
                .setTitle("Sweepstakes Results")
                .setImage(SWEEPSTAKES)
                .setColor(Color.GREEN)
                .setDescription(String.format("Congratulations %s you won today's sweepstakes worth 4000 bastard points!", member.getAsMention()))
                .build();

        philJda.getTextChannelsByName("bastard-of-the-week", false)
                .get(0)
                .sendMessage(message)
                .queue();
    }

    @Scheduled(cron = "0 5 1 * * ?", zone = "GMT")
    public void taxes() {
        List<DiscordUser> allUsers = discordUserRepository.findAll();
        allUsers.sort((u1, u2) -> Long.compare(u2.getXp(), u1.getXp())); // Descending sort

        long totalTaxes = 0;
        StringBuilder description = new StringBuilder();
        for (DiscordUser user : allUsers) {
            if (user.getXp() > 0) {
                try {
                    long taxRate = ThreadLocalRandom.current().nextInt(5, 16);
                    long taxes = BigDecimal.valueOf(user.getXp()).multiply(new BigDecimal("0.0" + taxRate)).longValue();
                    totalTaxes += taxes;
                    Member memberById = philJda.getGuilds().get(0).retrieveMemberById(user.getId()).complete();
                    if (memberById != null && hasRole(memberById, Constants.EIGHTEEN_PLUS)) {
                        takePointsFromMember(taxes, memberById);

                        description
                                .append("Collected ")
                                .append(NumberFormat.getIntegerInstance().format(taxes))
                                .append(" points (")
                                .append(taxRate)
                                .append("%) from <@!")
                                .append(user.getId())
                                .append(">\n");
                    }
                } catch (Exception e) {
                    logger.error("Failed to tax user [id={}]", user.getId(), e);
                }
            }
        }

        long startTime = System.currentTimeMillis();
        int index = allUsers.size() - 1;
        Member member = null;
        while (index >= 0 && member == null && System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(30)) {
            DiscordUser winningUser = allUsers.get(index);

            try {
                Member memberById = philJda.getGuilds().get(0).retrieveMemberById(winningUser.getId()).complete();
                if (memberById != null
                        && hasRole(memberById, Constants.EIGHTEEN_PLUS)
                        && winningUser.getXp() > 0
                        && winningUser.getUpdateTime().isAfter(LocalDateTime.now().minusHours(24))) {
                    member = memberById;
                }
            } catch (Exception ignored) {}
        }

        String title = "Tax time! The following taxes have been paid to Phil";
        if (member != null) {
            title = "Tax time! The following taxes have been paid to " + member.getEffectiveName();
        } else {
            member = philJda.getGuilds().get(0).getSelfMember();
        }

        givePointsToMember(totalTaxes, member);

        MessageEmbed message = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description.toString())
                .setImage(TAXES)
                .setColor(Color.RED)
                .build();

        philJda.getTextChannelsByName("bastard-of-the-week", false)
                .get(0)
                .sendMessage(message)
                .queue();
    }

    @Scheduled(cron = "0 5 2 * * ?", zone = "GMT")
    public void robinhood() {
        List<DiscordUser> allUsers = discordUserRepository.findAll();
        allUsers.sort((u1, u2) -> Long.compare(u2.getXp(), u1.getXp())); // Descending sort

        long totalRecovered = 0;
        StringBuilder description = new StringBuilder();
        for (DiscordUser user : allUsers) {
            if (user.getXp() > 0) {
                try {
                    long taxRateRecoveryAmountPercentage = ThreadLocalRandom.current().nextInt(5, 16);
                    long recoveredTaxes = BigDecimal.valueOf(user.getXp()).multiply(new BigDecimal("0.0" + taxRateRecoveryAmountPercentage)).longValue();
                    totalRecovered += recoveredTaxes;
                    Member memberById = philJda.getGuilds().get(0).retrieveMemberById(user.getId()).complete();
                    if (memberById != null && hasRole(memberById, Constants.EIGHTEEN_PLUS)) {
                        givePointsToMember(recoveredTaxes, memberById);

                        description
                                .append("Recovered ")
                                .append(NumberFormat.getIntegerInstance().format(recoveredTaxes))
                                .append(" points (")
                                .append(taxRateRecoveryAmountPercentage)
                                .append("%) to <@!")
                                .append(user.getId())
                                .append(">\n");
                    }
                } catch (Exception e) {
                    logger.error("Failed to robinhood user [id={}]", user.getId(), e);
                }
            }
        }

        description.append("\nI recovered a total of ")
                .append(NumberFormat.getIntegerInstance().format(totalRecovered))
                .append(" points and gave them back to the bastards!");

        String title = "Robinhood! The following taxes have been returned";
        MessageEmbed message = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description.toString())
                .setImage(ROBINHOOD)
                .setColor(Color.ORANGE)
                .build();

        philJda.getTextChannelsByName("bastard-of-the-week", false)
                .get(0)
                .sendMessage(message)
                .queue();
    }

    @Scheduled(cron = "0 20 * * * ?", zone = "GMT")
    public void swiper() {
        if (SWIPER_AWAITING != null) {
            Optional<DiscordUser> discordUser = discordUserRepository.findById(SWIPER_AWAITING);
            SWIPER_AWAITING = null;

            MessageEmbed message = new EmbedBuilder()
                    .setTitle("Swiper No Swiping")
                    .setDescription("Congratulations, <@!" + philJda.getSelfUser().getId() + "> is a moron so nobody loses any points")
                    .setColor(Color.GREEN)
                    .setImage(NO_SWIPING)
                    .build();

            if (discordUser.isPresent()) {
                if (DID_SOMEONE_SAVE_FROM_SWIPER) {
                    DID_SOMEONE_SAVE_FROM_SWIPER = false;

                    message = new EmbedBuilder()
                            .setTitle("Swiper No Swiping")
                            .setDescription("Congratulations, you scared swiper away from <@!" + discordUser.get().getId() + ">")
                            .setColor(Color.GREEN)
                            .setImage(NO_SWIPING)
                            .build();
                } else {
                    try {
                        Member memberById = philJda.getGuilds().get(0).retrieveMemberById(discordUser.get().getId()).complete();

                        if (memberById != null) {
                            takePointsFromMember(1000, memberById);
                            message = new EmbedBuilder()
                                    .setTitle("Swiper Escaped!")
                                    .setDescription("You didn't save <@!" + discordUser.get().getId() + "> in time, they lost 1000 points")
                                    .setColor(Color.RED)
                                    .setImage(SWIPER_WON)
                                    .build();
                        }
                    } catch (Exception e) {
                        logger.error("Exception looking up swiper victim [id={}] after they were not saved", discordUser.get().getId(), e);
                    }
                }
            }

            philJda.getTextChannelsByName("bastard-of-the-week", false)
                    .get(0)
                    .sendMessage(message)
                    .queue();
            return;
        }

        List<DiscordUser> allUsers = discordUserRepository.findAll();
        Collections.shuffle(allUsers);

        long startTime = System.currentTimeMillis();
        Member member = null;
        while (member == null && System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(30)) {
            DiscordUser winningUser = pickRandom(allUsers);

            try {
                Member memberById = philJda.getGuilds().get(0).retrieveMemberById(winningUser.getId()).complete();
                if (memberById != null
                        && hasRole(memberById, Constants.EIGHTEEN_PLUS)
                        && winningUser.getXp() > 0
                        && winningUser.getUpdateTime().isAfter(LocalDateTime.now().minusHours(24))) {
                    member = memberById;
                }
            } catch (Exception ignored) {}
        }

        if (member == null) {
            MessageEmbed message = new EmbedBuilder()
                    .setTitle("Swiper Was Spotted Nearby")
                    .setDescription("Swiper was spotted nearby, be on the lookout for him")
                    .setColor(Color.GREEN)
                    .setImage(NO_SWIPING)
                    .build();

            philJda.getTextChannelsByName("bastard-of-the-week", false)
                    .get(0)
                    .sendMessage(message)
                    .queue();
            return;
        }

        SWIPER_AWAITING = member.getId();

        MessageEmbed message = new EmbedBuilder()
                .setTitle("Swiper Was Spotted Nearby")
                .setDescription("Swiper is trying to steal from <@!" + member.getId() + ">\nType 'SWIPER NO SWIPING' in this channel to stop him!")
                .setColor(Color.YELLOW)
                .setImage(SWIPER_WON)
                .build();

        philJda.getTextChannelsByName("bastard-of-the-week", false)
                .get(0)
                .sendMessage(message)
                .queue();
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "GMT")
    public void boost() {
        TextChannel bastardOfTheWeekChannel = philJda.getTextChannelsByName("bastard-of-the-week", false)
                .get(0);

        if (BOOST_AWAITING) {
            BOOST_AWAITING = false;

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            StringBuilder description = new StringBuilder();
            discordUserRepository.findAll()
                    .stream()
                    .filter(u -> u.getAcceptedBoost().isAfter(oneHourAgo))
                    .forEach(u -> {
                        try {
                            Member memberLookedUp = philJda.getGuilds().get(0).retrieveMemberById(u.getId()).complete();
                            if (memberLookedUp == null) {
                                throw new RuntimeException("member not found");
                            }

                            givePointsToMember(1000, memberLookedUp);
                            description.append("Gave 1000 points to <@!")
                                    .append(u.getId())
                                    .append(">\n");
                        } catch (Exception e) {
                            logger.error("Failed to give boost points to user [id={}]", u.getId(), e);
                            description.append("OOPS: Unable to give points to <@!")
                                    .append(u.getId())
                                    .append(">\n");
                        }
                    });

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setImage(BENEVOLENT_GOD)
                    .setTitle("Boost Blitz Complete")
                    .setDescription(description.toString())
                    .setColor(Color.GREEN)
                    .build();
            bastardOfTheWeekChannel.sendMessage(messageEmbed).queue();
        }

        if (ThreadLocalRandom.current().nextInt(100) < 25) {
            BOOST_AWAITING = true;
            bastardOfTheWeekChannel
                    .sendMessage("BOOST BLITZ!!! Type `boost` in this channel within the next hour to be boosted by 1,000 points")
                    .queue();
        }
    }

    public void givePointsToMember(long pointsToGive, Member member, DiscordUser user) {
        if (!hasRole(member, Constants.EIGHTEEN_PLUS)) {
            return;
        }

        user.setXp(user.getXp() + pointsToGive);
        user = discordUserRepository.save(user);
        assignRolesIfNeeded(member, user);
    }

    public void givePointsToMember(long pointsToGive, Member member) {
        givePointsToMember(pointsToGive, member, getDiscordUserByMember(member));
    }

    public void voiceJoined(Member member) {
        if (!hasRole(member, Constants.EIGHTEEN_PLUS)) {
            return;
        }

        DiscordUser user = getDiscordUserByMember(member);
        user.setVoiceJoined(LocalDateTime.now());
        discordUserRepository.save(user);
    }

    public void voiceLeft(Member member) {
        if (!hasRole(member, Constants.EIGHTEEN_PLUS)) {
            return;
        }

        DiscordUser user = getDiscordUserByMember(member);
        LocalDateTime timeTheyJoinedVoice = user.getVoiceJoined();

        if (timeTheyJoinedVoice == null) {
            return;
        }

        long minutes = ChronoUnit.MINUTES.between(timeTheyJoinedVoice, LocalDateTime.now());
        long points = minutes * NORMAL_MSG_POINTS;
        user.setVoiceJoined(null);
        discordUserRepository.save(user);

        givePointsToMember(points, member);
    }

    public void acceptedBoost(Member member) {
        DiscordUser discordUser = getDiscordUserByMember(member);
        discordUser.setAcceptedBoost(LocalDateTime.now());
        discordUserRepository.save(discordUser);
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getAuthor().isBot() || !hasRole(event.getMember(), Constants.EIGHTEEN_PLUS)) {
            return;
        }

        String msgContent = event.getMessage().getContentRaw();

        if (msgContent.startsWith("!!bastard help")) {
            if (hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
                event.replyInDm(simpleEmbed("Bastard Games Help", adminHelp));
            } else {
                event.replyInDm(simpleEmbed("Bastard Games Help", userHelp));
            }
        } else if (msgContent.startsWith("!!bastard rank")) {
            showRank(event);
        } else if (msgContent.startsWith("!!bastard up")) {
            upvote(event);
        } else if (msgContent.startsWith("!!bastard down")) {
            downvote(event);
        } else if (msgContent.startsWith("!!bastard give")) {
            give(event);
        } else if (msgContent.startsWith("!!bastard take")) {
            take(event);
        } else if (msgContent.startsWith("!!bastard set")) {
            set(event);
        } else if (msgContent.startsWith("!!bastard leaderboard")) {
            leaderboard(event);
        } else if (msgContent.startsWith("!!bastard steal")) {
            steal(event);
        } else if (msgContent.startsWith("!!bastard slots")) {
            slots(event);
        } else if (msgContent.startsWith("!!bastard flip")) {
            flip(event);
        } else if (msgContent.startsWith("!!bastard reset")) {
            if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
                event.replyError("You do not have permission to use this command");
                return;
            }

            if (!AWAITING_RESET_CONFIRMATION) {
                event.reply(simpleEmbed("Reset The Games", "Are you sure you want to reset the bastard games? This will reset everyone's points. If you are sure, type `!!bastard reset confirm`"));
                AWAITING_RESET_CONFIRMATION = true;
            } else {
                if (!msgContent.startsWith("!!bastard reset confirm")) {
                    event.reply(simpleEmbed("Abort Reset", "Bastard reset aborted. You must type `!!bastard reset` and then confirm it with `!!bastard reset confirm`."));
                    AWAITING_RESET_CONFIRMATION = false;
                    return;
                }
                reset(event);
                AWAITING_RESET_CONFIRMATION = false;
            }
        } else if (msgContent.startsWith("!!bastard")){
            event.replyError("Unrecognized bastard command");
        } else {
            if (BOOST_AWAITING && StringUtils.containsIgnoreCase(msgContent, "boost") && "bastard-of-the-week".equalsIgnoreCase(event.getChannel().getName())) {
                acceptedBoost(event.getMember());
                return;
            }

            if (SWIPER_AWAITING != null && StringUtils.containsIgnoreCase(msgContent, "no swiping")
                    && "bastard-of-the-week".equalsIgnoreCase(event.getChannel().getName())) {
                DID_SOMEONE_SAVE_FROM_SWIPER = true;
                return;
            }

            Message message = event.getMessage();

            DiscordUser discordUser = getDiscordUserByMember(event.getMember());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextMsgBonusTime = discordUser.getLastMessageBonus().plus(3, ChronoUnit.MINUTES);

            boolean bonus = CollectionUtils.isNotEmpty(message.getAttachments())
                    || CollectionUtils.isNotEmpty(message.getEmbeds())
                    || CollectionUtils.isNotEmpty(message.getEmotes());

            long pointsToGive = NORMAL_MSG_POINTS;
            if ("bot-space".equals(event.getChannel().getName())) {
                pointsToGive = 1;
            } else if (bonus && now.isAfter(nextMsgBonusTime)) {
                pointsToGive = 150;
                discordUser.setLastMessageBonus(now);
            }

            givePointsToMember(pointsToGive, event.getMember(), discordUser);
        }
    }

    private DiscordUser getDiscordUserByMember(Member member) {
        String userId = member.getId();
        Optional<DiscordUser> optionalUserEntity = discordUserRepository.findById(userId);

        if (!optionalUserEntity.isPresent()) {
            DiscordUser newUser = new DiscordUser();
            newUser.setId(userId);
            optionalUserEntity = Optional.of(discordUserRepository.save(newUser));
        }

        return optionalUserEntity.get();
    }

    private void flip(CommandEvent event) {
        DiscordUser discordUser = getDiscordUserByMember(event.getMember());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextFlipTime = discordUser.getLastFlip().plus(3, ChronoUnit.MINUTES);
        if (now.isBefore(nextFlipTime)) {
            Duration duration = Duration.between(now, nextFlipTime);

            if (duration.getSeconds() < 60) {
                event.replyError("You must wait " + (duration.getSeconds() + 1) + " seconds before flipping again");
            } else {
                event.replyError("You must wait " + (duration.toMinutes() + 1) + " minutes before flipping again");
            }
            return;
        }
        discordUser.setLastFlip(now);
        discordUserRepository.save(discordUser);

        int randomNumber = ThreadLocalRandom.current().nextInt(100);

        if (randomNumber < 5) {
            takePointsFromMember(4, event.getMember());
            event.reply(simpleEmbed("Flip", "I don't feel like flipping a coin, I'm taking 4 bastard points from you \uD83D\uDCA9"));
        } else if (randomNumber % 2 == 0) {
            givePointsToMember(20, event.getMember());
            event.reply(simpleEmbed("Flip", "I flipped a coin and it landed on heads, here's 20 bastard points \uD83D\uDCB0"));
        } else {
            takePointsFromMember(10, event.getMember());
            event.reply(simpleEmbed("Flip", "I flipped a coin and it landed on tails, you lost 10 bastard points \uD83D\uDE2C"));
        }
    }

    private void slots(CommandEvent event) {
        Member member = event.getMember();
        DiscordUser discordUser = getDiscordUserByMember(member);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSlotsTime = discordUser.getLastSlots().plus(3, ChronoUnit.MINUTES);
        if (now.isBefore(nextSlotsTime)) {
            Duration duration = Duration.between(now, nextSlotsTime);

            if (duration.getSeconds() < 60) {
                event.replyError("You must wait " + (duration.getSeconds() + 1) + " seconds before playing slots again");
            } else {
                event.replyError("You must wait " + (duration.toMinutes() + 1) + " minutes before playing slots again");
            }
            return;
        }

        discordUser.setLastSlots(now);

        String one = pickRandom(SLOTS);
        String two = pickRandom(SLOTS);
        String three = pickRandom(SLOTS);

        if (one.equalsIgnoreCase(two) && two.equalsIgnoreCase(three)) {
            givePointsToMember(10000, member, discordUser);
            event.reply(simpleEmbed(SLOT_MACHINE + " WINNER WINNER!! " + SLOT_MACHINE, "%s\n%s%s%s \nYou won 10,000 bastard points!",
                    member.getAsMention(), one, two, three));
        } else if (one.equalsIgnoreCase(two) || one.equalsIgnoreCase(three) || two.equalsIgnoreCase(three)) {
            givePointsToMember(50, member, discordUser);
            event.reply(simpleEmbed(SLOT_MACHINE + " CLOSE ENOUGH! " + SLOT_MACHINE, "%s\n%s%s%s \nYou got 2 out of 3! You won 50 bastard points!",
                    member.getAsMention(), one, two, three));
        } else {
            event.reply(simpleEmbed(SLOT_MACHINE + " Better luck next time! " + SLOT_MACHINE, "%s\n%s%s%s",
                    member.getAsMention(), one, two, three));
            discordUserRepository.save(discordUser);
        }
    }

    private void steal(CommandEvent event) {
        List<Member> mentionedUsers = event.getMessage().getMentionedMembers();

        if (CollectionUtils.isEmpty(mentionedUsers) || mentionedUsers.size() > 1) {
            event.replyError("Please only mention one user. Example `!!bastard steal @incogmeato`");
            return;
        }

        Member mentionedMember = mentionedUsers.get(0);
        if (event.getMember().getId().equalsIgnoreCase(mentionedMember.getId())) {
            event.replyError("You can't steal from yourself");
            return;
        }

        if (!hasRole(mentionedMember, Constants.EIGHTEEN_PLUS)) {
            event.replyError("You can't steal from non 18+ members");
            return;
        }

        DiscordUser mentionedUser = getDiscordUserByMember(mentionedMember);
        if (mentionedUser.getXp() <= 0) {
            event.replyError("You can't steal from them, they don't have any points");
            return;
        }

        DiscordUser discordUser = getDiscordUserByMember(event.getMember());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextStealTime = discordUser.getLastSteal().plus(3, ChronoUnit.MINUTES);
        if (now.isBefore(nextStealTime)) {
            Duration duration = Duration.between(now, nextStealTime);

            if (duration.getSeconds() < 60) {
                event.replyError("You must wait " + (duration.getSeconds() + 1) + " seconds before stealing again");
            } else {
                event.replyError("You must wait " + (duration.toMinutes() + 1) + " minutes before stealing again");
            }
            return;
        }
        discordUser.setLastSteal(now);
        discordUserRepository.save(discordUser);

        int randomNumber;
        if ((randomNumber = ThreadLocalRandom.current().nextInt(1, 101)) < 30) {
            event.reply(simpleEmbed("Steal", "You attempt to steal points from %s and fail miserably, you pay them %s points to forget this ever happened. \uD83D\uDE2C",
                    mentionedMember, randomNumber));
            takePointsFromMember(randomNumber, event.getMember());
            givePointsToMember(randomNumber, mentionedMember);
        } else if ((randomNumber = ThreadLocalRandom.current().nextInt(1, 101)) < 30) {
            event.reply(simpleEmbed("Steal", "You successfully stole %s points from %s \uD83D\uDCB0", randomNumber, mentionedMember));
            takePointsFromMember(randomNumber, mentionedMember);
            givePointsToMember(randomNumber, event.getMember());
        } else if (ThreadLocalRandom.current().nextInt(1, 101) < 80) {
            event.reply(simpleEmbed("Steal", "You failed to steal from %s but escaped unnoticed. Take 2 points for your troubles. \uD83D\uDCB0", mentionedMember));
            givePointsToMember(2, event.getMember());
        } else {
            randomNumber = ThreadLocalRandom.current().nextInt(1, 21);
            event.reply(simpleEmbed("Steal", "I steal %s points, from both of you! Phil wins! \uD83D\uDE2C", randomNumber));
            takePointsFromMember(randomNumber, event.getMember());
            takePointsFromMember(randomNumber, mentionedMember);
        }
    }

    private void takePointsFromMember(long pointsToTake, Member member) {
        if (!hasRole(member, Constants.EIGHTEEN_PLUS)) {
            return;
        }

        DiscordUser user = getDiscordUserByMember(member);
        user.setXp(Math.max(0, user.getXp() - pointsToTake));
        user = discordUserRepository.save(user);
        assignRolesIfNeeded(member, user);
    }

    private void setPointsForMember(long points, Member member) {
        DiscordUser user = getDiscordUserByMember(member);
        user.setXp(points);
        user = discordUserRepository.save(user);
        assignRolesIfNeeded(member, user);
    }

    private void showRank(CommandEvent event) {
        Member member = event.getMember();
        if (CollectionUtils.isNotEmpty(event.getMessage().getMentionedUsers())) {
            member = event.getMessage().getMentionedMembers().get(0);
        }

        if (!hasRole(member, Constants.EIGHTEEN_PLUS)) {
            event.reply(simpleEmbed("Your Rank", "You can't see your rank because it appears you don't have the 18+ role"));
            return;
        }

        DiscordUser user = getDiscordUserByMember(member);
        Role role = assignRolesIfNeeded(member, user);

        Rank[] allRanks = Rank.values();
        Rank rank = Rank.byXp(user.getXp());
        Rank nextRank = (rank.ordinal() > allRanks.length - 1) ? rank : allRanks[rank.ordinal() + 1];

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setImage(rank.getRankUpImage())
                .setTitle("Level " + rank.getLevel() + ": " + rank.getRoleName())
                .setColor(role.getColor())
                .setDescription(rank.getRankUpMessage().replace("<name>", member.getAsMention()).replace("<rolename>", rank.getRoleName()) +
                        "\n\nYou have " + NumberFormat.getIntegerInstance().format(user.getXp()) + " total bastard points.\n\n" +
                        "The next level is level " +
                        (rank == nextRank
                                ? " LOL NVM YOU'RE THE TOP LEVEL."
                                : nextRank.getLevel() + ": " + nextRank.getRoleName() + ".") +
                        "\n You have " + NumberFormat.getIntegerInstance().format((nextRank.getLevel() * Rank.LVL_MULTIPLIER) - user.getXp()) + " points to go." +
                        "\n\nBest of Luck in the Bastard Games!")
                .build();

        event.reply(messageEmbed);
    }

    private void leaderboard(CommandEvent event) {
        List<DiscordUser> bastardUsers = discordUserRepository.findAll();

        bastardUsers.sort((u1, u2) -> Long.compare(u2.getXp(), u1.getXp())); // Descending sort

        AtomicInteger place = new AtomicInteger(0);
        StringBuilder description = new StringBuilder();
        bastardUsers.stream().limit(10).forEachOrdered(bastardUser -> {
            description.append(LEADERBOARD_MEDALS[place.getAndIncrement()])
                    .append(": <@!")
                    .append(bastardUser.getId())
                    .append("> - ")
                    .append(NumberFormat.getIntegerInstance().format(bastardUser.getXp()))
                    .append('\n');
        });

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setTitle("Leaderboard")
                .setDescription(description.toString())
                .build();

        event.reply(messageEmbed);
    }

    private MessageEmbed simpleEmbed(String title, String format, Object... args) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(String.format(format, args))
                .build();
    }

    private void upvote(CommandEvent event) {
        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();

        if (CollectionUtils.isEmpty(mentionedMembers) || mentionedMembers.size() > 1) {
            event.replyError("Please mention one user to upvote. Example `!!bastard up @incogmeato`");
            return;
        }

        if (!hasRole(mentionedMembers.get(0), Constants.EIGHTEEN_PLUS)) {
            event.replyError(mentionedMembers.get(0).getEffectiveName() + " is not participating in the bastard games");
            return;
        }

        if (event.getMember().getId().equalsIgnoreCase(mentionedMembers.get(0).getId())) {
            event.replyError("You can't upvote yourself");
            return;
        }

        DiscordUser discordUser = getDiscordUserByMember(event.getMember());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextVoteTime = discordUser.getLastVote().plus(3, ChronoUnit.MINUTES);
        if (now.isBefore(nextVoteTime)) {
            Duration duration = Duration.between(now, nextVoteTime);

            if (duration.getSeconds() < 60) {
                event.replyError("You must wait " + (duration.getSeconds() + 1) + " seconds before up/down-voting again");
            } else {
                event.replyError("You must wait " + (duration.toMinutes() + 1) + " minutes before up/down-voting again");
            }
            return;
        }
        discordUser.setLastVote(now);
        discordUserRepository.save(discordUser);

        givePointsToMember(500, mentionedMembers.get(0));

        event.replySuccess("Successfully upvoted " + mentionedMembers.get(0).getEffectiveName());
    }

    private void downvote(CommandEvent event) {
        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();

        if (CollectionUtils.isEmpty(mentionedMembers) || mentionedMembers.size() > 1) {
            event.replyError("Please mention one user to downvote. Example `!!bastard down @incogmeato`");
            return;
        }

        if (!hasRole(mentionedMembers.get(0), Constants.EIGHTEEN_PLUS)) {
            event.replyError(mentionedMembers.get(0).getEffectiveName() + " is not participating in the bastard games");
            return;
        }

        if (event.getMember().getId().equalsIgnoreCase(mentionedMembers.get(0).getId())) {
            event.replyError("You can't downvote yourself");
            return;
        }

        DiscordUser discordUser = getDiscordUserByMember(event.getMember());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextVoteTime = discordUser.getLastVote().plus(3, ChronoUnit.MINUTES);
        if (now.isBefore(nextVoteTime)) {
            Duration duration = Duration.between(now, nextVoteTime);

            if (duration.getSeconds() < 60) {
                event.replyError("You must wait " + (duration.getSeconds() + 1) + " seconds before up/down-voting again");
            } else {
                event.replyError("You must wait " + (duration.toMinutes() + 1) + " minutes before up/down-voting again");
            }
            return;
        }
        discordUser.setLastVote(now);
        discordUserRepository.save(discordUser);

        takePointsFromMember(100, mentionedMembers.get(0));

        event.replySuccess("Successfully downvoted " + mentionedMembers.get(0).getEffectiveName());
    }

    private void give(CommandEvent event) {
        if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
            event.replyError("You do not have permission to use this command");
            return;
        }

        String msgContent = event.getMessage().getContentRaw();

        String stripped = msgContent.replace("!!bastard give", "").trim();
        String[] split = stripped.split("\\s+");

        if (split.length != 2) {
            event.replyError("Badly formatted command. Example `!!bastard give 100 @incogmeato`");
            return;
        }

        long pointsToGive;
        try {
            pointsToGive = Long.parseLong(split[0]);
        } catch (NumberFormatException e) {
            event.replyError("Failed to parse the number you provided.");
            return;
        }

        if (pointsToGive < 0) {
            event.replyError("Please provide a positive number");
            return;
        }

        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();

        if (CollectionUtils.isEmpty(mentionedMembers) || mentionedMembers.size() > 1) {
            event.replyError("Please specify only one user. Example `!!bastard give 100 @incogmeato`");
            return;
        }

        if (!hasRole(mentionedMembers.get(0), Constants.EIGHTEEN_PLUS)) {
            event.replyError(mentionedMembers.get(0).getEffectiveName() + " is not participating in the bastard games");
            return;
        }

        givePointsToMember(pointsToGive, mentionedMembers.get(0));

        event.replyFormatted("Added %s xp to %s", NumberFormat.getIntegerInstance().format(pointsToGive), mentionedMembers.get(0).getEffectiveName());
    }

    private void take(CommandEvent event) {
        if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
            event.replyError("You do not have permission to use this command");
            return;
        }

        String msgContent = event.getMessage().getContentRaw();

        String stripped = msgContent.replace("!!bastard take", "").trim();
        String[] split = stripped.split("\\s+");

        if (split.length != 2) {
            event.replyError("Badly formatted command. Example `!!bastard take 100 @incogmeato`");
            return;
        }

        long pointsToTake;
        try {
            pointsToTake = Long.parseLong(split[0]);
        } catch (NumberFormatException e) {
            event.replyError("Failed to parse the number you provided.");
            return;
        }

        if (pointsToTake < 0) {
            event.replyError("Please provide a positive number");
            return;
        }

        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();

        if (CollectionUtils.isEmpty(mentionedMembers) || mentionedMembers.size() > 1) {
            event.replyError("Please specify only one user. Example `!!bastard take 100 @incogmeato`");
            return;
        }

        if (!hasRole(mentionedMembers.get(0), Constants.EIGHTEEN_PLUS)) {
            event.replyError(mentionedMembers.get(0).getEffectiveName() + " is not participating in the bastard games");
            return;
        }

        takePointsFromMember(pointsToTake, mentionedMembers.get(0));

        event.replyFormatted("Removed %s xp from %s", NumberFormat.getIntegerInstance().format(pointsToTake), mentionedMembers.get(0).getEffectiveName());
    }

    private void set(CommandEvent event) {
        if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
            event.replyError("You do not have permission to use this command");
            return;
        }

        String msgContent = event.getMessage().getContentRaw();

        String stripped = msgContent.replace("!!bastard set", "").trim();
        String[] split = stripped.split("\\s+");

        if (split.length != 2) {
            event.replyError("Badly formatted command. Example `!!bastard set 100 @incogmeato`");
            return;
        }

        long pointsToSet;
        try {
            pointsToSet = Long.parseLong(split[0]);
        } catch (NumberFormatException e) {
            event.replyError("Failed to parse the number you provided.");
            return;
        }

        if (pointsToSet < 0) {
            event.replyError("Please provide a positive number");
            return;
        }

        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();

        if (CollectionUtils.isEmpty(mentionedMembers) || mentionedMembers.size() > 1) {
            event.replyError("Please specify only one user. Example `!!bastard set 100 @incogmeato`");
            return;
        }

        if (!hasRole(mentionedMembers.get(0), Constants.EIGHTEEN_PLUS)) {
            event.replyError(mentionedMembers.get(0).getEffectiveName() + " is not participating in the bastard games");
            return;
        }

        setPointsForMember(pointsToSet, mentionedMembers.get(0));

        event.replyFormatted("Set xp to %s for %s", NumberFormat.getIntegerInstance().format(pointsToSet), mentionedMembers.get(0).getEffectiveName());
    }

    private void reset(CommandEvent event) {
        if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
            event.replyError("You do not have permission to use this command");
            return;
        }

        for (DiscordUser discordUser : discordUserRepository.findAll()) {
            discordUser.setXp(0);
            discordUser = discordUserRepository.save(discordUser);

            try {
                final DiscordUser finalDiscordUser = discordUser;
                event.getGuild().retrieveMemberById(discordUser.getId()).submit().whenComplete((member, err) -> {
                    if (member != null) {
                        assignRolesIfNeeded(member, finalDiscordUser);
                    } else {
                        event.replyError("Failed to reset roles for user with discord id: <@!" + finalDiscordUser.getId() + '>');
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to reset roles for user with [id={}]", discordUser.getId(), e);
                event.replyError("Failed to reset roles for user with discord id: <@!" + discordUser.getId() + '>');
            }
        }

        event.replySuccess("Reset the bastard games");
    }

    private Role assignRolesIfNeeded(Member member, DiscordUser user) {
        if (!hasRole(member, Constants.EIGHTEEN_PLUS)) {
            return null;
        }

        Rank newRank = Rank.byXp(user.getXp());
        Role newRole = member.getGuild().getRolesByName(newRank.getRoleName(), true).get(0);

        if (!hasRole(member, newRank)) {
            Set<String> allRoleNames = Rank.getAllRoleNames();
            Guild guild = member.getGuild();

            member.getRoles().stream().filter(r -> allRoleNames.contains(r.getName())).forEach(roleToRemove -> {
                guild.removeRoleFromMember(member, roleToRemove).complete();
            });

            guild.addRoleToMember(member, newRole).complete();

            if (newRank != Rank.CINNAMON_ROLL) {
                TextChannel announcementsChannel = member.getGuild().getTextChannelsByName("bastard-of-the-week", false).get(0);

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setImage(newRank.getRankUpImage())
                        .setTitle("Level Change!")
                        .setColor(newRole.getColor())
                        .setDescription(newRank.getRankUpMessage().replace("<name>", member.getAsMention()).replace("<rolename>", newRank.getRoleName()))
                        .build();

                announcementsChannel.sendMessage(messageEmbed).queue();
            }
        }

        return newRole;
    }

    private static boolean hasRole(Member member, Rank rank) {
        return hasRole(member, rank.getRoleName());
    }

    private static boolean hasRole(Member member, String role) {
        return member.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(role));
    }

    private static <T> T pickRandom(Collection<T> collection) {
        int index = ThreadLocalRandom.current().nextInt(collection.size());
        Iterator<T> iterator = collection.iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        return iterator.next();
    }

}
