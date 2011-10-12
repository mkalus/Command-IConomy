//Copyright (C) 2011  Ryan Michela
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.ryanmichela.cmdiconomy;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;

import com.fernferret.allpay.GenericBank;

public class CIListener extends PlayerListener {

	private CIPlugin plugin;
	private PriceCache pc;
	
	public CIListener(CIPlugin plugin, PriceCache pc) {
		this.plugin = plugin;
		this.pc = pc;
		
		if(verbose()) {
			log().info("[Command iConomy] Verbose mode enabled.");
		}
	}
	
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		chargePlayerForCommand(event);
	}
	
	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		chargePlayerForCommand(event);
	}
	
	private void chargePlayerForCommand(PlayerChatEvent event) {
	
		if(event.getPlayer() == null) return;
		
		try {
			// Is the player exempt by permission?
			if (event.getPlayer().hasPermission("commandiconomy.free")) return;
		} catch (NoClassDefFoundError e) {
			// Permissions not installed, fall back to ops
			//if(event.getPlayer().isOp()) return; ops may also want to pay :-)
		}
		
		// Is this command one we are charging for?	
		for(String re : pc.getExpressions()) {
			
			Pattern p = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(event.getMessage());
			if(m.find()) {
				// get bank
				GenericBank bank = plugin.getAllPay().getEconPlugin();
				
				System.out.println(event.getMessage());
				// Is this command currently cooling down?
				if(!CooldownClock.TimerExpired(event.getPlayer(), re)) {
					return;
				}
				// Does the command have a cost of zero?
				double cost = pc.getCost(re);
				if(cost == 0f){
					System.out.println("[debug]cost = 0 , finish");
					return;
				}
				System.out.println("[debug]cost > 0 deduct");
				// Does the player have sufficient funds?
				double holdings = bank.getBalance(event.getPlayer(), -1);
				
				if (holdings < cost) {
					String msg = config().getString("InsuficientFundsMessage", "Insuficent funds. {cost} needed.");
					msg = msg.replaceAll("\\{cost\\}", bank.getFormattedAmount(event.getPlayer(), cost, -1));
					if(verbose()) {
						log().info("[Command iConomy] " + event.getPlayer().getName() + " has insuficent funds to invoke " + event.getMessage());
					}
					event.getPlayer().sendMessage(ChatColor.RED + msg);
					event.setCancelled(true);
					return;
				}

				///////////////////////////////////////////////////
				
				// All checks passed - deduct funds
				bank.pay(event.getPlayer(), cost, -1);
				//iConomy.getAccount(pName).getHoldings().subtract(cost);
				//String msg = config().getString("AccountDeductedMessage", "Charged {cost}");
				if(verbose()) {
					log().info("[Command iConomy] " + event.getPlayer().getName() + " was charged " + bank.getFormattedAmount(event.getPlayer(), cost, -1) + " for " + event.getMessage());
				}
				//msg = msg.replaceAll("\\{cost\\}", iConomy.format(cost));
				//event.getPlayer().sendMessage(ChatColor.GREEN + msg);
				
				// Start the cooldown timer
				CooldownClock.StartTimer(event.getPlayer(), re, pc.getCooldown(re));
				
				// If there is a pay to account, make a payment
				String payTo = config().getString("PayTo");
				if(payTo != null) {
					try {
						Player payableTo = plugin.getServer().getPlayer(payTo);
						if (payableTo == null) throw new NullPointerException();
						bank.give(payableTo, cost, -1);
						if(verbose()) {
							log().info("[Command iConomy] " + payTo + " was credited " + bank.getFormattedAmount(payableTo, cost, -1));
						}
					} catch (NullPointerException e) {
						log().severe("[Command iConomy] Cannot deposit funds into the account " + payTo + " because it does not exist!");
					}
				}
				return;
			}
		}	
	}

	private boolean verbose() {
		return plugin.getConfiguration().getBoolean("Verbose", false);
	}
	
	private Configuration config() {
		return plugin.getConfiguration();
	}
	
	private Logger log() {
		return plugin.getServer().getLogger();
	}
}
