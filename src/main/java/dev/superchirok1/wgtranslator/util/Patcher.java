package dev.superchirok1.wgtranslator.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.listener.RegionProtectionListener;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StringFlag;
import dev.superchirok1.wgtranslator.config.Configuration;
import dev.superchirok1.wgtranslator.translation.TranslationManager;
import lombok.experimental.UtilityClass;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

@UtilityClass
public class Patcher {

    static {
        ByteBuddyAgent.install();
    }

    public void denyMessage(String message) {
        StringFlag deny = Flags.DENY_MESSAGE;
        try {
            Field field = StringFlag.class.getDeclaredField("defaultValue");
            field.setAccessible(true);

            field.set(deny, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void patchListenerMethod() {
        new ByteBuddy()
                .redefine(RegionProtectionListener.class)
                .method(ElementMatchers.named("formatAndSendDenyMessage"))
                .intercept(MethodDelegation.to(Class.class))
                .make()
                .load(RegionProtectionListener.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    public class Class {
        @RuntimeType
        public static void formatAndSendDenyMessage(@AllArguments Object[] args) {
            String what = (String) args[0];
            LocalPlayer localPlayer = (LocalPlayer) args[1];
            String message = (String) args[2];

            if (message == null || message.isEmpty()) return;
            message = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(localPlayer, message);
            what = TranslationManager.getTranslation().components.get(what);
            message = message.replace("%what%", what);
            if (Configuration.displayDenyMessage == Configuration.DisplayMode.DEFAULT) {
                localPlayer.printRaw(message);
            } else {
                Player player = BukkitAdapter.adapt(localPlayer);
                player.sendActionBar(message);
            }
        }
    }

}
