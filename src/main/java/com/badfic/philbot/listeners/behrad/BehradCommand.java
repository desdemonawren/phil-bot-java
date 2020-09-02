package com.badfic.philbot.listeners.behrad;

import com.badfic.philbot.config.BaseConfig;
import com.badfic.philbot.config.BehradMarker;
import com.badfic.philbot.data.BaseResponsesConfig;
import com.badfic.philbot.data.behrad.BehradResponsesConfig;
import com.badfic.philbot.model.GenericBotResponsesConfigJson;
import com.badfic.philbot.repository.BehradResponsesConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class BehradCommand extends Command implements BehradMarker {

    private final boolean isTestEnvironment;
    private final BehradResponsesConfigRepository behradResponsesConfigRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public BehradCommand(ObjectMapper objectMapper, BaseConfig baseConfig, BehradResponsesConfigRepository behradResponsesConfigRepository)
            throws Exception {
        isTestEnvironment = "test".equalsIgnoreCase(baseConfig.nodeEnvironment);
        name = "behrad";
        help = "Any message containing `behrad` will make behrad respond with a random message if that channel is configured.\n" +
                "\t`!!behrad tts` responds with a random message but spoken via text-to-speech\n" +
                "\t`!!behrad nsfw add channel #channel` adds a channel to the list of channels nsfw behrad responds to\n" +
                "\t`!!behrad nsfw remove channel #channel` removes a channel from the list of channels nsfw behrad responds to\n" +
                "\t`!!behrad sfw add channel #channel` adds a channel to the list of channels normal behrad responds to\n" +
                "\t`!!behrad sfw remove channel #channel` removes a channel from the list of channels normal behrad responds to\n" +
                "\t`!!behrad nsfw add something something` adds 'something something' to the list of responses behrad has for nsfw channels\n" +
                "\t`!!behrad nsfw remove something something` removes 'something something' from the list of responses behrad has for nsfw channels\n" +
                "\t`!!behrad sfw add something something` adds 'something something' to the list of responses behrad has for normal channels\n" +
                "\t`!!behrad sfw remove something something` removes 'something something' from the list of responses behrad has for normal channels\n" +
                "\t`!!behrad nsfw config` responds with a json file of the nsfw config\n" +
                "\t`!!behrad sfw config` responds with a json file of the normal config\n";
        requiredRole = "Queens of the Castle";
        this.behradResponsesConfigRepository = behradResponsesConfigRepository;
        this.objectMapper = objectMapper;

        // seed data if needed
        if (!behradResponsesConfigRepository.findById(BaseResponsesConfig.SINGLETON_ID).isPresent()) {
            GenericBotResponsesConfigJson kidFriendlyConfig = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("behrad-kidFriendlyConfig.json"),
                    GenericBotResponsesConfigJson.class);
            GenericBotResponsesConfigJson nsfwConfig = objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("behrad-nsfwConfig.json"),
                    GenericBotResponsesConfigJson.class);
            nsfwConfig.getResponses().addAll(kidFriendlyConfig.getResponses());

            BehradResponsesConfig responsesConfig = new BehradResponsesConfig();
            responsesConfig.setId(BehradResponsesConfig.SINGLETON_ID);
            responsesConfig.setSfwConfig(kidFriendlyConfig);
            responsesConfig.setNsfwConfig(nsfwConfig);
            behradResponsesConfigRepository.save(responsesConfig);
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        Optional<BehradResponsesConfig> optionalConfig = behradResponsesConfigRepository.findById(BaseResponsesConfig.SINGLETON_ID);
        if (!optionalConfig.isPresent()) {
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", failed to read behrad entries from database :(").queue();
            return;
        }

        BehradResponsesConfig responsesConfig = optionalConfig.get();
        String msgContent = event.getMessage().getContentRaw();

        if (msgContent.startsWith("!!behrad")) {
            if (msgContent.startsWith("!!behrad nsfw add channel")) {
                List<TextChannel> mentionedChannels = event.getMessage().getMentionedChannels();
                if (CollectionUtils.isEmpty(mentionedChannels)) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a channel. `!!behrad nsfw add channel #cursed`").queue();
                    return;
                }

                if (mentionedChannels.size() > 1) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please only specify one channel. `!!behrad nsfw add channel #cursed`")
                            .queue();
                    return;
                }

                responsesConfig.getNsfwConfig().getChannels().add(mentionedChannels.get(0).getName());
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", saved " + mentionedChannels.get(0).getAsMention() + " to nsfw config")
                        .queue();
            } else if (msgContent.startsWith("!!behrad nsfw remove channel")) {
                List<TextChannel> mentionedChannels = event.getMessage().getMentionedChannels();
                if (CollectionUtils.isEmpty(mentionedChannels)) {
                    String channelName = msgContent.replace("!!behrad nsfw remove channel", "").trim();

                    if (StringUtils.isEmpty(channelName)) {
                        event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a channel. `!!behrad nsfw remove channel #general`")
                                .queue();
                        return;
                    }

                    responsesConfig.getSfwConfig().getChannels().remove(mentionedChannels.get(0).getName());
                    behradResponsesConfigRepository.save(responsesConfig);
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", removed " + channelName + " from nsfw config").queue();
                    return;
                }

                if (mentionedChannels.size() > 1) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please only specify one channel. `!!behrad nsfw remove channel #cursed`")
                            .queue();
                    return;
                }

                responsesConfig.getNsfwConfig().getChannels().remove(mentionedChannels.get(0).getName());
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", removed " + mentionedChannels.get(0).getAsMention() + " from nsfw config")
                        .queue();
            } else if (msgContent.startsWith("!!behrad sfw add channel")) {
                List<TextChannel> mentionedChannels = event.getMessage().getMentionedChannels();
                if (CollectionUtils.isEmpty(mentionedChannels)) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a channel. `!!behrad sfw add channel #general`").queue();
                    return;
                }

                if (mentionedChannels.size() > 1) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please only specify one channel. `!!behrad sfw add channel #general`")
                            .queue();
                    return;
                }

                responsesConfig.getSfwConfig().getChannels().add(mentionedChannels.get(0).getName());
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", saved " + mentionedChannels.get(0).getAsMention() + " to sfw config")
                        .queue();
            } else if (msgContent.startsWith("!!behrad sfw remove channel")) {
                List<TextChannel> mentionedChannels = event.getMessage().getMentionedChannels();
                if (CollectionUtils.isEmpty(mentionedChannels)) {
                    String channelName = msgContent.replace("!!behrad sfw remove channel", "").trim();

                    if (StringUtils.isEmpty(channelName)) {
                        event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a channel. `!!behrad sfw remove channel #general`")
                                .queue();
                        return;
                    }

                    responsesConfig.getSfwConfig().getChannels().remove(mentionedChannels.get(0).getName());
                    behradResponsesConfigRepository.save(responsesConfig);
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", removed " + channelName + " from sfw config").queue();
                    return;
                }

                if (mentionedChannels.size() > 1) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please only specify one channel. `!!behrad sfw remove channel #general`")
                            .queue();
                    return;
                }

                responsesConfig.getSfwConfig().getChannels().remove(mentionedChannels.get(0).getName());
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", removed " + mentionedChannels.get(0).getAsMention() + " from sfw config")
                        .queue();
            } else if (msgContent.startsWith("!!behrad nsfw add")) {
                String saying = msgContent.replace("!!behrad nsfw add", "").trim();
                if (saying.isEmpty()) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a saying to add").queue();
                    return;
                }
                responsesConfig.getNsfwConfig().getResponses().add(saying);
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", saved `" + saying + "` to nsfw config").queue();
            } else if (msgContent.startsWith("!!behrad nsfw remove")) {
                String saying = msgContent.replace("!!behrad nsfw remove", "").trim();
                if (saying.isEmpty()) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a saying to remove").queue();
                    return;
                }
                responsesConfig.getNsfwConfig().getResponses().remove(saying);
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", removed `" + saying + "` from nsfw config").queue();
            } else if (msgContent.startsWith("!!behrad sfw add")) {
                String saying = msgContent.replace("!!behrad sfw add", "").trim();
                if (saying.isEmpty()) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a saying to add").queue();
                    return;
                }
                responsesConfig.getSfwConfig().getResponses().add(saying);
                responsesConfig.getNsfwConfig().getResponses().add(saying);
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", saved `" + saying + "` to sfw config").queue();
            } else if (msgContent.startsWith("!!behrad sfw remove")) {
                String saying = msgContent.replace("!!behrad sfw remove", "").trim();
                if (saying.isEmpty()) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", please specify a saying to remove").queue();
                    return;
                }
                responsesConfig.getSfwConfig().getResponses().remove(saying);
                responsesConfig.getNsfwConfig().getResponses().remove(saying);
                behradResponsesConfigRepository.save(responsesConfig);
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", removed `" + saying + "` from sfw config").queue();
            } else if (msgContent.startsWith("!!behrad nsfw config")) {
                try {
                    event.getChannel().sendFile(objectMapper.writeValueAsBytes(responsesConfig.getNsfwConfig()), "nsfwconfig.json").queue();
                } catch (JsonProcessingException e) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", fatal error could not send nsfw config to you").queue();
                }
            } else if (msgContent.startsWith("!!behrad sfw config")) {
                try {
                    event.getChannel().sendFile(objectMapper.writeValueAsBytes(responsesConfig.getSfwConfig()), "sfwconfig.json").queue();
                } catch (JsonProcessingException e) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", fatal error could not send sfw config to you").queue();
                }
            } else if (msgContent.startsWith("!!behrad tts")) {
                getResponse(event, responsesConfig).ifPresent(response -> {
                    Message outboundMessage = new MessageBuilder(event.getAuthor().getAsMention() + ", " + response)
                            .setTTS(true)
                            .build();

                    event.getChannel().sendMessage(outboundMessage).queue();
                });
            } else {
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", unrecognized behrad command").queue();
            }
            return;
        }

        getResponse(event, responsesConfig).ifPresent(response -> {
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", " + response).queue();
        });
    }

    private Optional<String> getResponse(CommandEvent event, BehradResponsesConfig responsesConfig) {
        String msgContent = event.getMessage().getContentRaw();
        String channelName = event.getChannel().getName();
        Set<String> responses;
        if (isTestEnvironment) {
            if ("test-channel".equalsIgnoreCase(channelName)) {
                responses = responsesConfig.getNsfwConfig().getResponses();
            } else {
                return Optional.empty();
            }
        } else {
            if (responsesConfig.getSfwConfig().getChannels().contains(channelName)) {
                responses = responsesConfig.getSfwConfig().getResponses();
            } else if (responsesConfig.getNsfwConfig().getChannels().contains(channelName)) {
                responses = responsesConfig.getNsfwConfig().getResponses();
            } else {
                return Optional.empty();
            }
        }

        if (EmojiManager.containsEmoji(msgContent)) {
            Collection<Emoji> allEmoji = EmojiManager.getAll();
            Emoji emoji = pickRandom(allEmoji);
            return Optional.of(emoji.getUnicode());
        }

        return Optional.of(pickRandom(responses));
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