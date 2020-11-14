package com.badfic.philbot.listeners.phil.swampy;

import com.badfic.philbot.config.PhilMarker;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

@Component
public class TimerCommand extends BaseSwampy implements PhilMarker {

    public TimerCommand() {
        name = "timer";
        help = "`!!timer` a simple timer, only works in seconds. Max 5 minutes (300 seconds). Example:\n"+
                "`!!timer 30` a timer for 30 seconds, phil will let you know when it's complete.";
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");

        try {
            int time = Integer.parseInt(args[0]);

            if (time > 300) {
                event.replyError("Max is 300 seconds (5 minutes)");
                return;
            }

            String id = RandomStringUtils.randomAlphabetic(4);
            event.reply(simpleEmbed("Timer started for " + time + " seconds", null,
                    "https://cdn.discordapp.com/attachments/752665408770801737/777011911647690752/Webp.net-resizeimage.png", "timer id = " + id));
            event.getChannel().sendMessage(simpleEmbed("Time's up", null,
                    "https://cdn.discordapp.com/attachments/752665408770801737/777011404536414228/Webp.net-resizeimage.jpg", "timer id = " + id))
                    .queueAfter(time, TimeUnit.SECONDS);
        } catch (NumberFormatException e) {
            event.replyError("Badly formatted command. Example `!!timer 30` for a 30 second timer.");
        }
    }
}
