package com.badfic.philbot.listeners.phil.swampy;

import com.badfic.philbot.config.Constants;
import com.badfic.philbot.config.PhilMarker;
import com.badfic.philbot.data.phil.SwampyGamesConfig;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

@Component
public class Storytime extends BaseSwampy implements PhilMarker {

    private final List<String> story;

    public Storytime() throws Exception {
        name = "storytime";
        story = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("storytime.txt")))) {
            while (reader.ready()) {
                story.add(reader.readLine());
            }
        }
    }

    @Override
    public void execute(CommandEvent event) {
        Optional<SwampyGamesConfig> optionalConfig = swampyGamesConfigRepository.findById(SwampyGamesConfig.SINGLETON_ID);
        if (!optionalConfig.isPresent()) {
            return;
        }
        SwampyGamesConfig swampyGamesConfig = optionalConfig.get();

        if (!event.getArgs().isEmpty()) {
            String[] args = event.getArgs().split("\\s+");
            if (ArrayUtils.getLength(args) > 0) {
                if ("reset".equalsIgnoreCase(args[0])) {
                    if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
                        event.replyError("You must be a mod to use storytime parameters. Non-mods can only use `!!storytime`");
                        return;
                    }
                    swampyGamesConfig.setStoryTimeCounter(0);
                    swampyGamesConfigRepository.save(swampyGamesConfig);
                    event.replySuccess("reset story time counter");
                } else if ("set".equalsIgnoreCase(args[0])) {
                    if (!hasRole(event.getMember(), Constants.ADMIN_ROLE)) {
                        event.replyError("You must be a mod to use storytime parameters. Non-mods can only use `!!storytime`");
                        return;
                    }
                    try {
                        int index = Integer.parseInt(args[1]);
                        if (index > -1) {
                            swampyGamesConfig.setStoryTimeCounter(index);
                            swampyGamesConfigRepository.save(swampyGamesConfig);
                            event.replySuccess("set story time counter to " + index);
                        } else {
                            event.replyError("index must be greater than or equal to 0");
                        }
                    } catch (Exception e) {
                        event.replyError("unable to parse number provided to `!!storytime set` command. Example `!!storytime set 22`");
                        return;
                    }
                } else {
                    event.replyError("Unrecognized command. Example `!!storytime` or `!!storytime reset` or `!!storytime set 22`");
                }
                return;
            }
        }

        event.reply(story.get(swampyGamesConfig.getStoryTimeCounter()));
        swampyGamesConfig.setStoryTimeCounter((story.size() - 1) > swampyGamesConfig.getStoryTimeCounter() ? swampyGamesConfig.getStoryTimeCounter() + 1 : 0);
        swampyGamesConfigRepository.save(swampyGamesConfig);
    }

}
