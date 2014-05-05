/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.watchtower;

import static org.springframework.boot.ansi.AnsiElement.DEFAULT;
import static org.springframework.boot.ansi.AnsiElement.FAINT;
import static org.springframework.boot.ansi.AnsiElement.GREEN;

import java.io.PrintStream;

import org.springframework.boot.ansi.AnsiOutput;

/**
 * <p>Title: Banner</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.Banner</code></p>
 */

public class Banner  {

	private static final String[] BANNER = { "",
		" __    __        _         _      _                             ",
		"/ / /\\ \\ \\ __ _ | |_  ___ | |__  | |_  ___ __      __ ___  _ __ ",
		"\\ \\/  \\/ // _` || __|/ __|| '_ \\ | __|/ _ \\\\ \\ /\\ / // _ \\| '__|",
		" \\  /\\  /| (_| || |_| (__ | | | || |_| (_) |\\ V  V /|  __/| |   ",
		"  \\/  \\/  \\__,_| \\__|\\___||_| |_| \\__|\\___/  \\_/\\_/  \\___||_|   "};
	
	private static final String SPRING_BOOT = " :: Scripted Monitoring Engine :: ";

	private static final int STRAP_LINE_SIZE = 42;
	
	public static void main(String[] args) {
		write(System.out);
	}

	/**
	 * Write the banner to the specified print stream.
	 * @param printStream the output print stream
	 */
	public static void write(PrintStream printStream) {
		for (String line : BANNER) {
			printStream.println(line);
		}
		String version = Banner.class.getPackage().getImplementationVersion();
		version = (version == null ? "" : " (v" + version + ")");
		String padding = "";
		while (padding.length() < STRAP_LINE_SIZE
				- (version.length() + SPRING_BOOT.length())) {
			padding += " ";
		}

		printStream.println(AnsiOutput.toString(GREEN, SPRING_BOOT, DEFAULT, padding,
				FAINT, version));
		printStream.println();
	}

}
