package au.gov.nsw.records.digitalarchive.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandExecutor {

	public static String exec(String[] args) throws IOException {
		String line;
		StringBuffer sb = new StringBuffer();

		Process p = Runtime.getRuntime().exec(args);
		BufferedReader input = new BufferedReader
				(new InputStreamReader(p.getInputStream()));
		while ((line = input.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		input.close();

		return sb.toString();
	}
}
