package com.badfic.philbot.listeners.john;

import com.badfic.philbot.config.Constants;
import com.badfic.philbot.data.phil.Reminder;
import com.badfic.philbot.data.phil.ReminderRepository;
import com.badfic.philbot.data.phil.SnarkyReminderResponse;
import com.badfic.philbot.data.phil.SnarkyReminderResponseRepository;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JohnMessageListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Pattern JOHN_PATTERN = Pattern.compile("\\b(john|constantine|johnno|johnny|hellblazer)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMINDER_PATTER = Pattern.compile("\\b(remind me in |remind <@![0-9]+> in )[0-9]+\\b", Pattern.CASE_INSENSITIVE);
    private static final ConcurrentMap<Long, Pair<String, Long>> LAST_WORD_MAP = new ConcurrentHashMap<>();

    @Resource
    private JohnCommand johnCommand;

    @Resource
    private ReminderRepository reminderRepository;

    @Resource
    private SnarkyReminderResponseRepository snarkyReminderResponseRepository;

    @Scheduled(cron = "0 * * * * ?", zone = "GMT")
    public void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Reminder reminder : reminderRepository.findAll()) {
            if (reminder.getDueDate().isBefore(now) || reminder.getDueDate().isEqual(now)) {
                TextChannel textChannelById = johnCommand.getJohnJda().getTextChannelById(reminder.getChannelId());

                if (textChannelById != null) {
                    textChannelById.sendMessage("(reminder #" + reminder.getId() + ") <@!" + reminder.getUserId() + "> " + reminder.getReminder()).queue();
                    reminderRepository.deleteById(reminder.getId());
                }
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String msgContent = event.getMessage().getContentRaw();

        if (msgContent.startsWith("!!") || event.getAuthor().isBot()) {
            return;
        }

        long channelId = event.getMessage().getChannel().getIdLong();
        LAST_WORD_MAP.compute(channelId, (key, oldValue) -> {
            if (oldValue == null) {
                return new ImmutablePair<>(msgContent, 1L);
            }
            if (oldValue.getLeft().equalsIgnoreCase(msgContent)) {
                if (oldValue.getRight() + 1 >= 5) {
                    johnCommand.getJohnJda().getTextChannelById(channelId).sendMessage(msgContent).queue();
                    return new ImmutablePair<>(msgContent, 0L);
                }

                return new ImmutablePair<>(msgContent, oldValue.getRight() + 1);
            } else {
                return new ImmutablePair<>(msgContent, 1L);
            }
        });

        if (REMINDER_PATTER.matcher(msgContent).find()) {
            addReminder(event.getMessage());
            return;
        }

        if ("ayy".equalsIgnoreCase(msgContent)) {
            johnCommand.getJohnJda().getTextChannelById(event.getChannel().getId()).sendMessage("lmao").queue();
            return;
        }
        if ("slang".equalsIgnoreCase(msgContent)) {
            johnCommand.getJohnJda().getTextChannelById(event.getChannel().getId()).sendMessage("You're an absolute whopper").queue();
        }
        if ("stughead".equalsIgnoreCase(msgContent)) {
            johnCommand.getJohnJda().getTextChannelById(event.getChannel().getId()).sendMessage("\uD83D\uDC40").queue();
        }
        if ("Africa".equalsIgnoreCase(msgContent)) {
            johnCommand.getJohnJda().getTextChannelById(event.getChannel().getId()).sendMessage("I BLESS THE RAINS DOWN IN AAAAFRICAAA").queue();
        }
        if ("cool".equalsIgnoreCase(msgContent)) {
            johnCommand.getJohnJda().getTextChannelById(event.getChannel().getId()).sendMessage("cool cool cool").queue();
        }

        if (JOHN_PATTERN.matcher(msgContent).find()) {
            johnCommand.execute(new CommandEvent(event, null, null));
            return;
        }
    }

    private void addReminder(Message message) {
        try {
            String reminder;
            int remindIdx = StringUtils.indexOf(message.getContentRaw(), "remind");

            Member member = message.getMember();
            if (CollectionUtils.isNotEmpty(message.getMentionedMembers())) {
                member = message.getMentionedMembers().get(0);

                reminder = message.getContentRaw().substring(remindIdx + ("remind <@!" + member.getIdLong() + "> in ").length());
            } else {
                reminder = message.getContentRaw().substring(remindIdx + "remind me in ".length());
            }
            String[] split = reminder.split("\\s+");
            int number = Integer.parseInt(split[0]);
            String temporal = split[1];
            reminder = reminder.substring((split[0] + split[1]).length() + 1).trim();

            if (StringUtils.startsWithIgnoreCase(reminder, "to")) {
                reminder = reminder.replace("to", "").trim();
            }

            LocalDateTime dueDate = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

            if (StringUtils.containsIgnoreCase(temporal, "minute")) {
                dueDate = dueDate.plus(number, ChronoUnit.MINUTES);
            } else if (StringUtils.containsIgnoreCase(temporal, "hour")) {
                dueDate = dueDate.plus(number, ChronoUnit.HOURS);
            } else if (StringUtils.containsIgnoreCase(temporal, "day")) {
                dueDate = dueDate.plus(number, ChronoUnit.DAYS);
            }

            Reminder savedReminder = reminderRepository.save(new Reminder(member.getIdLong(), message.getChannel().getIdLong(), reminder, dueDate));
            SnarkyReminderResponse snarkyReminderResponse = Constants.pickRandom(snarkyReminderResponseRepository.findAll());
            johnCommand.getJohnJda().getTextChannelById(message.getChannel().getId())
                    .sendMessage("(reminder #" + savedReminder.getId() + ") " +
                            snarkyReminderResponse.getResponse().replace("<name>", "<@!" + message.getAuthor().getId() + ">"))
                    .queue();
        } catch (Exception e) {
            logger.error("Exception trying to parse a reminder", e);
        }
    }

}
