package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.BannedWordFilter;
import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.MatchmakingServer;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class BannedWordsCommand extends DiscordCommand {

    private String command_name = "bannedwords";
    private String command_description = "Manages the banned word list used for name filtering and chat censoring. Mod role required.";
    private String subcommand_add = "add";
    private String subcommand_remove = "remove";
    private String subcommand_list = "list";
    private String command_option_word = "word";
    private String command_option_match_type = "match_type";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOption> add = event.getOption(subcommand_add);
        if (add.isPresent())
            return doAdd(event, add.get());
        Optional<ApplicationCommandInteractionOption> remove = event.getOption(subcommand_remove);
        if (remove.isPresent())
            return doRemove(event, remove.get());
        if (event.getOption(subcommand_list).isPresent())
            return doList(event);
        return event.reply("Unknown subcommand.").withEphemeral(true);
    }

    private Mono<Void> doAdd(ChatInputInteractionEvent event, ApplicationCommandInteractionOption sub) {
        String word = getSubOptionString(sub, command_option_word);
        String match_type = getSubOptionString(sub, command_option_match_type);
        if (word == null || match_type == null)
            return event.reply("Missing word or match type.").withEphemeral(true);
        word = word.trim().toLowerCase();
        if (!word.matches("[a-z0-9]{3,64}"))
            return event.reply(
                    "Invalid word: use 3-64 letters/digits, no spaces or punctuation.").withEphemeral(true);
        if (!BannedWordFilter.MATCH_SUBSTRING.equals(match_type) && !BannedWordFilter.MATCH_EXACT.equals(match_type))
            return event.reply("Invalid match type.").withEphemeral(true);

        if (!DBInterface.addBannedWord(word, match_type))
            return event.reply("'" + word + "' is already on the banned word list.").withEphemeral(true);
        BannedWordFilter.invalidateCache();
        MatchmakingServer.getLogger().info(
                "Banned word '" + word + "' (" + match_type + ") added by Discord moderator " + event.getInteraction().getUser().getUsername());
        return event.reply("Added '" + word + "' (" + match_type + ") to the banned word list.").withEphemeral(true);
    }

    private Mono<Void> doRemove(ChatInputInteractionEvent event, ApplicationCommandInteractionOption sub) {
        String word = getSubOptionString(sub, command_option_word);
        if (word == null)
            return event.reply("Missing word.").withEphemeral(true);
        word = word.trim().toLowerCase();

        if (!DBInterface.removeBannedWord(word))
            return event.reply("'" + word + "' is not on the banned word list.").withEphemeral(true);
        BannedWordFilter.invalidateCache();
        MatchmakingServer.getLogger().info(
                "Banned word '" + word + "' removed by Discord moderator " + event.getInteraction().getUser().getUsername());
        return event.reply("Removed '" + word + "' from the banned word list.").withEphemeral(true);
    }

    private Mono<Void> doList(ChatInputInteractionEvent event) {
        List<String[]> words = DBInterface.getBannedWords();
        if (words.isEmpty())
            return event.reply("The banned word list is empty.").withEphemeral(true);
        StringBuilder sb = new StringBuilder();
        for (String[] entry : words) {
            String formatted = entry[0] + " (" + entry[1] + ")";
            // Keep under Discord's 2000 character message limit
            if (sb.length() + formatted.length() > 1900) {
                sb.append("... and more");
                break;
            }
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(formatted);
        }
        return event.reply(sb.toString()).withEphemeral(true);
    }

    private static String getSubOptionString(ApplicationCommandInteractionOption sub, String option_name) {
        return sub.getOption(option_name).flatMap(ApplicationCommandInteractionOption::getValue).map(
                ApplicationCommandInteractionOptionValue::asString).orElse(null);
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        // spotless:off
        ApplicationCommandOptionData word_option = ApplicationCommandOptionData.builder()
                .name(command_option_word)
                .description("The word, plain letters/digits only")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .build();
        ApplicationCommandOptionChoiceData substring_choice = ApplicationCommandOptionChoiceData.builder()
                .name("substring")
                .value(BannedWordFilter.MATCH_SUBSTRING)
                .build();
        ApplicationCommandOptionChoiceData exact_choice = ApplicationCommandOptionChoiceData.builder()
                .name("exact")
                .value(BannedWordFilter.MATCH_EXACT)
                .build();
        ApplicationCommandOptionData match_type_option = ApplicationCommandOptionData.builder()
                .name(command_option_match_type)
                .description("substring matches anywhere (only for words with no innocent uses); exact matches whole names/words")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .addChoice(substring_choice)
                .addChoice(exact_choice)
                .build();
        ApplicationCommandOptionData add_subcommand = ApplicationCommandOptionData.builder()
                .name(subcommand_add)
                .description("Adds a word to the banned word list")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(word_option)
                .addOption(match_type_option)
                .build();
        ApplicationCommandOptionData remove_subcommand = ApplicationCommandOptionData.builder()
                .name(subcommand_remove)
                .description("Removes a word from the banned word list")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(word_option)
                .build();
        ApplicationCommandOptionData list_subcommand = ApplicationCommandOptionData.builder()
                .name(subcommand_list)
                .description("Shows the banned word list")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .build();
        return ApplicationCommandRequest.builder()
                .name(command_name)
                .description(command_description)
                .defaultMemberPermissions(BAN_MEMBERS_PERMISSION)
                .addOption(add_subcommand)
                .addOption(remove_subcommand)
                .addOption(list_subcommand)
                .build();
        // spotless:on
    }
}
