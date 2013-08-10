package me.ryanhamshire.PopulationDensity.utils;


public class LoginQueueEntry {
	public String playerName;
	private int priority;
	private long lastRefreshed;
	
	public LoginQueueEntry(String playerName, int priority, long lastRefreshed) {
		this.setPriority(priority);
		this.playerName = playerName;
		this.setLastRefreshed(lastRefreshed);
	}

	public long getLastRefreshed() {
		return lastRefreshed;
	}

	public void setLastRefreshed(long lastRefreshed) {
		this.lastRefreshed = lastRefreshed;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
