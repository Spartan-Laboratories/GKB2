package com.spartanlabs.bottools.main;

import com.spartanlabs.bottools.main.EventAction.*;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Responder {
	Logger log = LoggerFactory.getLogger(this.getClass());
	@SuppressWarnings("rawtypes")
	private HashMap<Class, List<EventAction>> actionMap = new HashMap<Class, List<EventAction>>();

	Responder(){
		getSubclasses(Event.class).forEach(subclass -> actionMap.put(subclass, new ArrayList<EventAction>()));
		int x = 1;
		log.info("Created Responder");
	}
	
	void actOn(Event event) {
		actionMap.get(event.getClass()).forEach(eventAction -> eventAction.perform(event));
	}
	public void addOnGuildJoinAction(GuildJoinAction onEventAction) {
		actionMap.get(GuildJoinEvent.class).add(onEventAction);
	}
	public void addOnGuildUpdateNameAction(GuildUpdateNameAction onEventAction) {
		actionMap.get(GuildUpdateNameEvent.class).add(onEventAction);
	}
	public void addOnGuildMemberJoinAction(GuildMemberJoinAction onEventAction) {
		actionMap.get(GuildMemberJoinEvent.class).add(onEventAction);
	}
	public void addOnUserUpdateOnlineStatusAction(UserUpdateOnlineStatusAction eventAction) {
		actionMap.get(UserUpdateOnlineStatusEvent.class).add(eventAction);
	}
	public void addOnMessageReactionAddAction(MessageReactionAddAction onEventAction) {
		actionMap.get(MessageReactionAddEvent.class).add(onEventAction);
	}
	public void addOnMessageReceivedAction(MessageReceivedAction onEventAction) {
		actionMap.get(MessageReceivedEvent.class).add(onEventAction);
	}
	public void addOnMessageDeleteAction(MessageDeleteAction onEventAction) {
		actionMap.get(MessageDeleteEvent.class).add(onEventAction);
	}
	public void addOnSlashCommandInteractionAction(SlashCommandInteractionAction onEventAction) {
		actionMap.get(SlashCommandInteractionEvent.class).add(onEventAction);
	}
	public void addOnUserContextInteractionAction(UserContextInteractionAction onEventAction) {
		actionMap.get(UserContextInteractionEvent.class).add(onEventAction);
	}
	public void addOnMessageContextInteractionAction(MessageContextInteractionAction onEventAction) {
		actionMap.get(MessageContextInteractionEvent.class).add(onEventAction);
	}
	private static List<Class> getSubclasses(Class superClass) {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(superClass));
		Set<BeanDefinition> components = provider.findCandidateComponents("net.dv8tion.jda.api.events");
		ArrayList<Class> subclasses = new ArrayList<Class>();
		
		for(BeanDefinition component: components) {
			Class c = null;
			try {
				c = Class.forName(component.getBeanClassName());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(c == superClass)
				continue;
			subclasses.add(c);
			subclasses.addAll(getSubclasses(c));
		}
		return subclasses;
	}
}
