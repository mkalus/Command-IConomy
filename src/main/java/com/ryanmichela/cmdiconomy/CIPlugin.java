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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.java.JavaPlugin;

import com.fernferret.allpay.AllPay;

public class CIPlugin extends JavaPlugin {
	AllPay allPay;
	
	/**
	 * @return allPay instance (singleton)
	 */
	public AllPay getAllPay() {
		if (allPay == null) allPay = new AllPay(this, "Command iConomy: ");
		return allPay;
	}

	private static PluginListener PluginListener = null;
	
	private Logger log;
	private File pricesFile;
	
	@Override
	public void onLoad() {
	}

	@Override
	public void onEnable() {
		//Initialize config files
		initFiles();

		// register enable events from other plugins
		PluginListener = new PluginListener();

		getServer().getPluginManager().registerEvent(Event.Type.PLUGIN_ENABLE,
				PluginListener, Priority.Monitor, this);

		try {
			PriceCache pc = new PriceCache(pricesFile);	
			CIListener listener = new CIListener(this, pc);
			getServer().getPluginManager().registerEvent(Type.PLAYER_COMMAND_PREPROCESS, listener , Priority.Lowest, this);
		
			log.info("[Command iConomy] Loaded.");
		} catch (Exception e) {
			log.log(Level.SEVERE, "[Command iConomy] Failed to process prices.config", e);
		}
	}
	
	@Override
	public void onDisable() {
		// TODO Auto-generated method stub

	}
	
	private void initFiles() {
		log = getServer().getLogger();
		pricesFile = new File(getDataFolder(), "prices.yml");

		// Initialize config directory
		if(!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		
		// Initialize prices.yml			
		if(!pricesFile.exists()) {
			try {
				log.info("[Command iConomy] Populating initial prices file");
				PrintStream out = new PrintStream(new FileOutputStream(pricesFile));
				out.println("# To charge for a command, list a matching regular expression below on its own");
				out.println("# line with the price, separated by a colon. For more info on regular expressions");
				out.println("# see http://www.regular-expressions.info/reference.html");
				out.println();
				out.println("# ^/tp: 10");
				out.close();
			} catch (IOException e) {
				log.severe("[Command iConomy] Error initializing prices file. You're on your own!");
			}
		}
		
		// Initialize config.yml
		File configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists()) {
			try {
				log.info("[Command iConomy] Populating initial config file");
				PrintStream out = new PrintStream(new FileOutputStream(configFile));
				out.println("Verbose: false");
				out.println("ChargeForChat: false");
				out.println("#PayTo: accountName");
				out.println();
				out.println("NoAccountMessage: No bank account.");
				out.println("InsuficientFundsMessage: Insuficent funds. {cost} needed.");
				out.println("AccountDeductedMessage: Charged {cost}");
				out.close();
			} catch (IOException e) {
				log.severe("[Command iConomy] Error initializing config file. You're on your own!");
			}
		}
	}
}
