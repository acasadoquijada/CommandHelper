package com.laytonsmith.abstraction.bukkit.events.drivers;

import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.abstraction.bukkit.entities.BukkitMCPlayer;
import com.laytonsmith.abstraction.bukkit.events.BukkitPlayerEvents.*;
import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.events.drivers.PlayerEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class BukkitPlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
		BukkitMCFoodLevelChangeEvent pke = new BukkitMCFoodLevelChangeEvent(e);
		EventUtils.TriggerListener(Driver.FOOD_LEVEL_CHANGED, "food_level_changed", pke);
    }
	
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerKick(PlayerKickEvent e) {
		BukkitMCPlayerKickEvent pke = new BukkitMCPlayerKickEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_KICK, "player_kick", pke);
    }
	
	@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
		BukkitMCPlayerBedEvent be = new BukkitMCPlayerBedEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_BED_EVENT, "player_enter_bed", be);
    }
	
	@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBedLeave(PlayerBedLeaveEvent e) {
		BukkitMCPlayerBedEvent be = new BukkitMCPlayerBedEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_BED_EVENT, "player_leave_bed", be);
    }
    
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
		BukkitMCPlayerLoginEvent ple = new BukkitMCPlayerLoginEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_LOGIN, "player_login", ple);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPreLogin(PlayerPreLoginEvent e) {
		BukkitMCPlayerPreLoginEvent pple = new BukkitMCPlayerPreLoginEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_PRELOGIN, "player_prelogin", pple);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent e) {
		BukkitMCPlayerJoinEvent pje = new BukkitMCPlayerJoinEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_JOIN, "player_join", pje);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent e) {
		BukkitMCPlayerInteractEvent pie = new BukkitMCPlayerInteractEvent(e);
		EventUtils.TriggerListener(Driver.PLAYER_INTERACT, "player_interact", pie);
		EventUtils.TriggerListener(Driver.PLAYER_INTERACT, "pressure_plate_activated", pie);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		BukkitMCPlayerRespawnEvent pre = new BukkitMCPlayerRespawnEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_SPAWN, "player_spawn", pre);
		// Reset player_move lastLocations
		for(Integer i : PlayerEvents.GetThresholdList()) {
			PlayerEvents.GetLastLocations(i).put(event.getPlayer().getName(), new BukkitMCLocation(event.getRespawnLocation()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		if(CommandHelperPlugin.self.interpreterListener
                .isInInterpreterMode(event.getPlayer().getName())){
            //They are in interpreter mode, so we want it to handle this, not everything else.
            return;
        }
		
		if(event.isAsynchronous()){
			//The async event gets priority, and if cancelled, doesn't trigger a normal player_chat event.
			BukkitMCPlayerChatEvent pce = new BukkitMCPlayerChatEvent(event);
			EventUtils.TriggerListener(Driver.PLAYER_CHAT, "async_player_chat", pce);

			if(event.isCancelled()){
				return;
			}
		}
		
		if (EventUtils.GetEvents(Driver.PLAYER_CHAT) != null
			&& !EventUtils.GetEvents(Driver.PLAYER_CHAT).isEmpty()) {
			if (event.isAsynchronous()) {
				//We have to do the full processing on the main server thread, and
				//block on it as well, so if we cancel it or something, the change
				//will actually take effect. The easiest way to do this is to cancel the
				//chat event, then re-run it on the main server thread. Since we're
				//registering on lowest, this will hopefully not cause any problems,
				//but if it does, tough. Barring play-dirty mode, there's not a whole
				//lot that can be done reasonably.
				
//				SortedSet<BoundEvent> events = EventUtils.GetEvents(Driver.PLAYER_CHAT);
//				Event driver = EventList.getEvent(Driver.PLAYER_CHAT, "player_chat");				
//				//Unfortunately, due to priority issues, if any event is syncronous, all of them
//				//have to be synchronous.
//				boolean canBeAsync = true;
//				boolean actuallyNeedToFire = false;
//				//If all the events are asynchronous, we can just run it as is.
//				for(BoundEvent b : events){
//					//We can't just use isSync here, because cancel and modify event,
//					//normally synchronous, aren't in this case, so we need to manually
//					//check the full function list.
//					for(Function f : b.getParseTree().getFunctions()){
//						if(f instanceof EventBinding.cancel || f instanceof EventBinding.modify_event){
//							continue;
//						}
//						if(f.runAsync() != null && f.runAsync() == false){
//							//Nope, can't be run async :(
//							canBeAsync = false;
//						}						
//					}
//					try {
//						if(driver.matches(b.getPrefilter(), new BukkitPlayerEvents.BukkitMCPlayerChatEvent(event))){
//							//Yeah, we need to fire it, so we have to continue
//							actuallyNeedToFire = true;
//						}
//					} catch (PrefilterNonMatchException ex) {
//						//No need to fire this one
//					}
//				}
//				
//				if(!actuallyNeedToFire){
//					//Yay! Prefilters finally actually optimized something!
//					return;
//				}
				
				//Until there is a more reliable way to detect isConst() on a parse tree, (that supports procs)
				//this must always be synchronous.
				boolean canBeAsync = false;
				if(canBeAsync){
					//Fire away!
					fireChat(event);
				} else {
					final AsyncPlayerChatEvent copy = new AsyncPlayerChatEvent(false, event.getPlayer(), event.getMessage(), event.getRecipients());
					copy.setFormat(event.getFormat());
					//event.setCancelled(true);
					Future f = Bukkit.getServer().getScheduler().callSyncMethod(CommandHelperPlugin.self, new Callable() {
						@Override
						public Object call() throws Exception {
							onPlayerChat(copy);
							return null;
						}
					});					
					while(true){
						try {
								f.get();
								break;
						} catch (InterruptedException ex) {
							//I don't know why this happens, but screw it, we're gonna try again, and it's gonna like it.
						} catch (ExecutionException ex) {
							Logger.getLogger(BukkitPlayerListener.class.getName()).log(Level.SEVERE, null, ex);
							break;
						}
					}
					event.setCancelled(copy.isCancelled());
					event.setMessage(copy.getMessage());
					event.setFormat(copy.getFormat());
				}

			} else {
				fireChat(event);
			}
		}
	}

	private void fireChat(AsyncPlayerChatEvent event) {
		BukkitMCPlayerChatEvent pce = new BukkitMCPlayerChatEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_CHAT, "player_chat", pce);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		BukkitMCPlayerQuitEvent pqe = new BukkitMCPlayerQuitEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_QUIT, "player_quit", pqe);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		BukkitMCPlayer currentPlayer = (BukkitMCPlayer) Static.GetPlayer(event.getPlayer().getName(), Target.UNKNOWN);
		//Apparently this happens sometimes, so prevent it
		if (!event.getFrom().equals(currentPlayer._Player().getWorld())) {
			BukkitMCWorldChangedEvent wce = new BukkitMCWorldChangedEvent(event);
			EventUtils.TriggerListener(Driver.WORLD_CHANGED, "world_changed", wce);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.getFrom().equals(event.getTo())) {
			return;
		}
		
		BukkitMCPlayerTeleportEvent pte = new BukkitMCPlayerTeleportEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_TELEPORT, "player_teleport", pte);
		if(!pte.isCancelled()) {
			// Reset player_move lastLocations
			for(Integer i : PlayerEvents.GetThresholdList()) {
				PlayerEvents.GetLastLocations(i).put(event.getPlayer().getName(), new BukkitMCLocation(event.getTo()));
			}
		}
	}
	

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPortalEnter(PlayerPortalEvent event) {
		BukkitMCPlayerPortalEvent pe = new BukkitMCPlayerPortalEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_PORTAL_TRAVEL, "player_portal_travel", pe);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConsume(PlayerItemConsumeEvent event) {
		BukkitMCPlayerItemConsumeEvent pic = new BukkitMCPlayerItemConsumeEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_CONSUME, "player_consume", pic);
	}
	
	@EventHandler(priority= EventPriority.LOWEST)
	public void onFish(PlayerFishEvent event) {
		BukkitMCPlayerFishEvent fish = new BukkitMCPlayerFishEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_FISH, "player_fish", fish);
	}
	
	@EventHandler(priority= EventPriority.LOWEST)
	public void onGamemodeChange(PlayerGameModeChangeEvent event) {
		BukkitMCGamemodeChangeEvent e = new BukkitMCGamemodeChangeEvent(event);
		EventUtils.TriggerListener(Driver.GAMEMODE_CHANGE, "gamemode_change", e);
	}
	
	@EventHandler(priority= EventPriority.LOWEST)
	public void onChatTab(PlayerChatTabCompleteEvent event) {
		BukkitMCChatTabCompleteEvent e = new BukkitMCChatTabCompleteEvent(event);
		EventUtils.TriggerListener(Driver.TAB_COMPLETE, "tab_complete_chat", e);
	}
	
	@EventHandler(priority= EventPriority.LOWEST)
	public void onExpChange(PlayerExpChangeEvent event) {
		BukkitMCExpChangeEvent e = new BukkitMCExpChangeEvent(event);
		EventUtils.TriggerListener(Driver.EXP_CHANGE, "exp_change", e);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerEditBook(PlayerEditBookEvent event) {
		BukkitMCPlayerEditBookEvent pebe = new BukkitMCPlayerEditBookEvent(event);
		EventUtils.TriggerListener(Driver.BOOK_EDITED, "book_edited", pebe);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		BukkitMCPlayerToggleFlightEvent ptfe = new BukkitMCPlayerToggleFlightEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_TOGGLE_FLIGHT, "player_toggle_flight", ptfe);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		BukkitMCPlayerToggleSneakEvent ptse = new BukkitMCPlayerToggleSneakEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_TOGGLE_SNEAK, "player_toggle_sneak", ptse);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		BukkitMCPlayerToggleSprintEvent ptse = new BukkitMCPlayerToggleSprintEvent(event);
		EventUtils.TriggerListener(Driver.PLAYER_TOGGLE_SPRINT, "player_toggle_sprint", ptse);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerMove(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		if(from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
			return;
		}
		String p = event.getPlayer().getName();
		for(Integer threshold : PlayerEvents.GetThresholdList()) {
			Map<String, MCLocation> lastLocations = PlayerEvents.GetLastLocations(threshold);
			if(!lastLocations.containsKey(p)) {
				lastLocations.put(p, new BukkitMCLocation(from));
				continue;
			}
			MCLocation last = lastLocations.get(p);
			if (!to.getWorld().getName().equals(last.getWorld().getName())) {
				lastLocations.put(p, new BukkitMCLocation(to));
				continue;
			}
			MCLocation movedTo = new BukkitMCLocation(to);
			if (last.distance(movedTo) > threshold) {
				BukkitMCPlayerMoveEvent pme = new BukkitMCPlayerMoveEvent(event, threshold, last);
				EventUtils.TriggerListener(Driver.PLAYER_MOVE, "player_move", pme);
				if (!pme.isCancelled()) {
					lastLocations.put(p, movedTo);
				}
			}
		}
	}
}
