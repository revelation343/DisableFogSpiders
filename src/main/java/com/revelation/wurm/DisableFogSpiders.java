package com.revelation.wurm;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisableFogSpiders implements WurmServerMod, Initable, ServerStartedListener {

	private static ClassPool classPool = HookManager.getInstance().getClassPool();
	private static Logger logger = Logger.getLogger(DisableFogSpiders.class.getName());

	@Override
	public void init() {
		try {
			disableFogSpiderInPoll();
		} catch (NotFoundException | CannotCompileException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	@Override
	public void onServerStarted() {
		JAssistClassData.voidClazz();
	}

	/**
	 * Change Zone.poll() to disable the creation of Fog spiders.
	 * Use ExprEditor to change getFog() so it always returns Float.MIN_VALUE and thus disables creation.
	 * was--
	 *      if (this.isOnSurface()) {
	 *          if (Server.getWeather().getFog() > 0.5f && Zone.fogSpiders.size() < Zones.worldTileSizeX / 10) {
	 *
	 *
	 * @throws NotFoundException JA related, forwarded.
	 * @throws CannotCompileException JA related, forwarded.
	 */
	private void disableFogSpiderInPoll() throws NotFoundException, CannotCompileException {
		final int[] successes = new int[]{0};
		JAssistClassData zone = new JAssistClassData("com.wurmonline.server.zones.Zone", classPool);
		JAssistMethodData poll =  new JAssistMethodData(zone, "(I)V", "poll");

		poll.getCtMethod().instrument(new ExprEditor() {
			@Override
			public void edit(MethodCall methodCall) throws CannotCompileException {
				if (Objects.equals("getFog", methodCall.getMethodName())){
					logger.log(Level.FINE, "Zone.class, poll(), installed hook at line: " + methodCall.getLineNumber());
					methodCall.replace("$_ = Float.MIN_VALUE;");
					successes[0] = 1;
				}
			}
		});
		evaluateChangesArray(successes, "fogNoSpawnFogSpiders");
	}

	private static void evaluateChangesArray(int[] ints, String option) {
		boolean changesSuccessful = Arrays.stream(ints).noneMatch(value -> value == 0);
		if (changesSuccessful) {
			logger.log(Level.INFO, option + " option changes SUCCESSFUL");
		} else {
			logger.log(Level.INFO, option + " option changes FAILURE");
			logger.log(Level.FINE, Arrays.toString(ints));
		}
	}
}