package com.badfic.philbot.listeners.keanu;

import com.badfic.philbot.config.Constants;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KeanuMessageListener extends ListenerAdapter {

    private static final Pattern KEANU_PATTERN = Constants.compileWords("keanu|reeves|neo|john wick|puppy|puppies|pupper|doggo|doge");
    private static final Multimap<String, Pair<Pattern, String>> USER_TRIGGER_WORDS = ImmutableMultimap.<String, Pair<Pattern, String>>builder()
            .put("323520695550083074", ImmutablePair.of(Constants.compileWords("child"), "Yes father?"))
            .build();

    private final KeanuCommand keanuCommand;

    @Autowired
    public KeanuMessageListener(KeanuCommand keanuCommand) {
        this.keanuCommand = keanuCommand;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String msgContent = event.getMessage().getContentRaw();

        if (msgContent.startsWith("!!") || event.getAuthor().isBot()) {
            return;
        }

        Constants.checkUserTriggerWords(event, USER_TRIGGER_WORDS);

        if (KEANU_PATTERN.matcher(msgContent).find()) {
            keanuCommand.execute(new CommandEvent(event, null, null));
            return;
        }
    }

}
