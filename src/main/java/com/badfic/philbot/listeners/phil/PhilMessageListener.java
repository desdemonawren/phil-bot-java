package com.badfic.philbot.listeners.phil;

import com.badfic.philbot.config.Constants;
import com.badfic.philbot.config.PhilMarker;
import com.badfic.philbot.listeners.antonia.AntoniaMessageListener;
import com.badfic.philbot.listeners.behrad.BehradMessageListener;
import com.badfic.philbot.listeners.keanu.KeanuMessageListener;
import com.badfic.philbot.listeners.phil.swampy.SwampyCommand;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PhilMessageListener extends ListenerAdapter implements PhilMarker {

    private static final Cache<String, Function<MessageReactionAddEvent, Boolean>> OUTSTANDING_REACTION_TASKS = CacheBuilder.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();
    private static final Pattern PHIL_PATTERN = Pattern.compile("\\b(phil|klemmer|phellen|cw|willip|schlemmer|pharole|klaskin|phreddie|klercury|philliam)\\b", Pattern.CASE_INSENSITIVE);

    private final PhilCommand philCommand;
    private final SwampyCommand swampyCommand;
    private final CommandClient philCommandClient;

    @Resource
    @Lazy
    private BehradMessageListener behradMessageListener;

    @Resource
    @Lazy
    private KeanuMessageListener keanuMessageListener;

    @Resource
    @Lazy
    private AntoniaMessageListener antoniaMessageListener;

    @Resource(name = "behradJda")
    @Lazy
    private JDA behradJda;

    @Resource(name = "keanuJda")
    @Lazy
    private JDA keanuJda;

    @Resource(name = "antoniaJda")
    @Lazy
    private JDA antoniaJda;

    @Autowired
    public PhilMessageListener(PhilCommand philCommand,
                               SwampyCommand swampyCommand,
                               @Qualifier("philCommandClient") CommandClient philCommandClient) {
        this.philCommand = philCommand;
        this.swampyCommand = swampyCommand;
        this.philCommandClient = philCommandClient;
    }

    public static void addReactionTask(String messageId, Function<MessageReactionAddEvent, Boolean> function) {
        OUTSTANDING_REACTION_TASKS.put(messageId, function);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        antoniaMessageListener.onMessageReceived(new MessageReceivedEvent(antoniaJda, event.getResponseNumber(), event.getMessage()));
        behradMessageListener.onMessageReceived(new MessageReceivedEvent(behradJda, event.getResponseNumber(), event.getMessage()));
        keanuMessageListener.onMessageReceived(new MessageReceivedEvent(keanuJda, event.getResponseNumber(), event.getMessage()));

        String msgContent = event.getMessage().getContentRaw();

        if (msgContent.startsWith("!!") || event.getAuthor().isBot()) {
            return;
        }

        swampyCommand.execute(new CommandEvent(event, "", philCommandClient));

        if (PHIL_PATTERN.matcher(msgContent).find()) {
            philCommand.execute(new CommandEvent(event, null, philCommandClient));
            return;
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        OUTSTANDING_REACTION_TASKS.asMap().computeIfPresent(event.getMessageId(), (key, function) -> {
            boolean taskIsComplete = function.apply(event);

            if (taskIsComplete) {
                return null;
            }
            return function;
        });
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        swampyCommand.voiceJoined(event.getMember());
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        swampyCommand.voiceLeft(event.getMember());
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        swampyCommand.emote(event);
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        if (event.getRoles().stream().anyMatch(r -> r.getName().equals(Constants.CHAOS_CHILDREN_ROLE) || r.getName().equals(Constants.EIGHTEEN_PLUS_ROLE))) {
            String mention = event.getMember().getAsMention();
            event.getGuild().getTextChannelsByName(Constants.SWAMPYS_CHANNEL, false).get(0).sendMessage(mention + " Congratulations on not being a newbie " +
                    "anymore my chaotic friend! Now you're entered into The Swampys, our server wide chaos games. " +
                    "You don't have to actively participate, but if you'd like to join the chaos with us, take a peek at what The Swampys has to offer.\n\n" +
                    "https://discordapp.com/channels/740999022340341791/761398315119280158/761411776561807410").queue();
        }
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        begone(event.getUser(), event);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        begone(event.getUser(), event);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        event.getGuild().loadMembers().onSuccess(members -> {
            if (CollectionUtils.size(members) == 100) {
                event.getGuild().getTextChannelsByName("announcements", false).stream().findAny().ifPresent(channel -> {
                    channel.sendMessage("Ah shit. I guess I should say something special since we hit 100 members..... uh......").queue(msg -> {
                        channel.sendMessage("https://media.giphy.com/media/xUOwGjPHOGcv9ddpYc/giphy.gif").queue();
                    });
                });
            }
        });
    }

    private void begone(User user, GenericGuildEvent event) {
        swampyCommand.removeFromGames(user.getId());
        Optional<TextChannel> announcementsChannel = event.getGuild().getTextChannelsByName("announcements", false).stream().findFirst();
        announcementsChannel.ifPresent(channel -> channel.sendMessage("Begone Bot " + user.getAsMention()).queue());
    }

}
