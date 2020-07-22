import com.wurmonline.server.*;
import com.wurmonline.server.items.*;
import com.wurmonline.server.sounds.SoundPlayer;
import com.wurmonline.shared.constants.ModelConstants;
import com.wurmonline.shared.constants.SoundNames;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class initiator implements WurmServerMod, Configurable, PreInitable, Initable, ServerStartedListener, ItemTemplatesCreatedListener {

    private static final Logger logger = Logger.getLogger(initiator.class.getName());

    // Timer to poll repeated action
    private static final long delayWindmills = TimeConstants.MINUTE_MILLIS;
    private static long lastPolledWindmills = 0;

    // Items to act upon
    private final ArrayList<Item> windmills = new ArrayList<>();

    public static WindmillTemplate[] windmillTemplates = {
            new WindmillTemplate("Windmill", SoundNames.TOOL_GRINDSTONE, ModelConstants.MODEL_SUPPLYDEPOT2, ItemList.flour, ItemList.wheat, 0, 20, 1000, TimeConstants.MINUTE_MILLIS)
    };

    @Override
    public void configure(Properties properties) {
    }

    @Override
    public void preInit() {
    }

    @Override
    public void init() {
        HookManager.getInstance().registerHook("com.wurmonline.server.Server", "run", "()V", new InvocationHandlerFactory() {

            @Override
            public InvocationHandler createInvocationHandler() {
                return new InvocationHandler() {

                    @Override
                    public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                        long now = System.currentTimeMillis();
                        if (now - lastPolledWindmills > delayWindmills) {
                            logger.log(Level.INFO, String.format("Polling windmills at %d, time since last poll : %d", lastPolledWindmills, now - lastPolledWindmills));
                            lastPolledWindmills = now;
                            pollResourcePoints();
                        }

                        return method.invoke(object, args);
                    }

                };
            }
        });

    }

    @Override
    public void onServerStarted() {
    }

    @Override
    public void onItemTemplatesCreated() {
    }

    private void pollResourcePoints() {

        // If no windmills are found, search for them
        if (windmills.size() == 0) {
            for (Item item : Items.getAllItems()) {
                for (WindmillTemplate template : windmillTemplates) {
                    if (item.getTemplateId() == template.templateID) {
                        windmills.add(item);
                        logger.info(String.format("Windmill located and remembered, with name: %s, and wurmid: %d", item.getName(), item.getWurmId()));
                    }
                }
            }
        }

        // Loop through known windmills and spawn items
        for (Item resourcePoint : windmills) {
            for (WindmillTemplate template : windmillTemplates) {
                if (resourcePoint.getTemplateId() == template.templateID) {
                    spawnItemSpawn(resourcePoint, template.templateProduce, template.templateConsume, template.templateSecondaryConsume, 30.0F, template.maxNum, template.maxItems);
                    SoundPlayer.playSound(template.sound, resourcePoint, 0);
                }
            }
        }
    }

    private void spawnItemSpawn(Item item, int templateProduce, int templateConsume, int templateSecondaryConsume, float qlValRange, int maxNums, int maxItems) {
        Byte material = null;
        Item[] currentItems = item.getAllItems(true);
        int produceTally = 0;
        int consumeTally = 0;
        int secondaryConsumeTally = 0;

        float[] consumeQLs = new float[maxNums];
        float[] secondaryConsumeQLs = new float[maxNums];

        for (Item i : currentItems) {
            if (templateProduce == i.getTemplateId() || templateProduce == 50) {
                produceTally++;
            } else if (templateConsume == i.getTemplateId()) {
                if (consumeTally < consumeQLs.length) {
                    consumeQLs[consumeTally] = i.getQualityLevel();
                }
                consumeTally++;
            } else if (templateSecondaryConsume == i.getTemplateId()) {
                if (secondaryConsumeTally < secondaryConsumeQLs.length) {
                    secondaryConsumeQLs[secondaryConsumeTally] = i.getQualityLevel();
                }
                secondaryConsumeTally++;
            }
        }

        if (templateConsume != 0) {
            maxNums = Math.min(maxNums, consumeTally);
        }

        if (templateSecondaryConsume != 0) {
            maxNums = Math.min(maxNums, secondaryConsumeTally);
        }

        if (produceTally + maxNums > maxItems) {
            return;
        }

        if (templateConsume != 0) {

            consumeTally = Math.min(maxNums, consumeTally);

            if (templateSecondaryConsume != 0) {
                secondaryConsumeTally = Math.min(maxNums, secondaryConsumeTally);
            }

            for (Item i : currentItems) {
                if (consumeTally > 0 && i.getTemplateId() == templateConsume) {
                    Items.destroyItem(i.getWurmId());
                    consumeTally--;
                }
            }

            for (Item i : currentItems) {

                if (secondaryConsumeTally > 0 && i.getTemplateId() == templateSecondaryConsume) {
                    Items.destroyItem(i.getWurmId());
                    secondaryConsumeTally--;
                }
            }
        }


        for (int nums = 0; nums < maxNums; nums++) {
            try {
                byte rrarity = 0;
                float newql = (float) 50.0 + Server.rand.nextFloat() * qlValRange;
                if (templateConsume != 0) {
                    newql = Math.min(newql, consumeQLs[nums]);
                }

                if (templateSecondaryConsume != 0) {
                    newql = Math.min(newql, secondaryConsumeQLs[nums]);
                }

                Item toInsert;

                if (material == null) {
                    toInsert = ItemFactory.createItem(templateProduce, newql, rrarity, "");
                } else {
                    toInsert = ItemFactory.createItem(templateProduce, newql, material, rrarity, "");
                }

                item.insertItem(toInsert, true);

            } catch (FailedException | NoSuchTemplateException ignored) {

            }

        }
    }

}