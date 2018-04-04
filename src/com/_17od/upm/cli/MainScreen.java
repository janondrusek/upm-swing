package com._17od.upm.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;

import com._17od.upm.crypto.InvalidPasswordException;
import com._17od.upm.database.AccountInformation;
import com._17od.upm.database.PasswordDatabase;
import com._17od.upm.database.PasswordDatabasePersistence;
import com._17od.upm.util.Preferences;
import com._17od.upm.util.Translator;

public class MainScreen {
	public static void main(String[] args) {
		try {
			Preferences.load();
			Translator.initialise();
			final double jvmVersion = Double.parseDouble(System.getProperty("java.specification.version"));
			if (jvmVersion < 1.8) {
				throw new IllegalStateException(Translator.translate("requireJava14", 1.8));
			}
			// Load the startup database, must be configured
			final String db = Preferences.get(Preferences.ApplicationOptions.DB_TO_LOAD_ON_STARTUP);
			if (db == null || Files.notExists(Paths.get(db))) {
				throw new IllegalArgumentException(Translator.translate("dbDoesNotExist", db));
			}
			final TextIO textIO = TextIoFactory.getTextIO();

			boolean passwordCorrect = false;
			String password = null;
			PasswordDatabasePersistence dbPers;
			PasswordDatabase database = null;
			while (!passwordCorrect) {
				password = textIO.newStringInputReader()
						.withInputMasking(true)
						.read(Translator.translate("enterDatabasePassword"));
				try {
					dbPers = new PasswordDatabasePersistence();
					database = dbPers.load(new File(db), password.toCharArray());
					passwordCorrect = true;
				} catch (InvalidPasswordException e) {
					continue;
				}
			}
			@SuppressWarnings("unchecked")
			final Map<String, AccountInformation> accountsHash = database.getAccountsHash();
			final List<String> accounts = new ArrayList<>(accountsHash.keySet());
			Collections.sort(accounts, String.CASE_INSENSITIVE_ORDER);
			final List<String> labels = new ArrayList<>(accounts.size());
			final int max = digits(accounts.size());
			for (int i = 0; i < accounts.size(); i++) {
				final StringBuilder sb = new StringBuilder();
				final int d = digits(i);
				for (int j = Math.max(1, d); j < max; j++) {
					sb.append(" ");
				}
				sb.append(i).append(": ").append(accounts.get(i));
				labels.add(sb.toString());
			}
			final int accountIndex = textIO.newIntInputReader()
					.withMinVal(1)
					.withMaxVal(accounts.size())
					.read(labels);
			final TextTerminal<?> terminal = textIO.getTextTerminal();
			final String accName = accounts.get(accountIndex);

			final AccountInformation accInfo = accountsHash.get(accName);
			terminal.printf("\n[%s] %s: %s\n", accName, accInfo.getUserId(), accInfo.getPassword());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int digits(int i) {
		int d = 0;
		while (i > 0) {
			i /= 10;
			d++;
		}
		return d;
	}
}
