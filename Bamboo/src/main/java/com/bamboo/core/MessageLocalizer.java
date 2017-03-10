package com.bamboo.core;

import java.util.Locale;

public interface MessageLocalizer {
	
	public String getLocalizedMessage(Locale locale, String key);
	
	public String[] getLocalizedMessages(Locale locale, String...keys);
	
}
