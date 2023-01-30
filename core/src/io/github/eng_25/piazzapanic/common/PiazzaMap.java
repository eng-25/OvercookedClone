package io.github.eng_25.piazzapanic.common;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.eng_25.piazzapanic.common.entity.Cook;
import io.github.eng_25.piazzapanic.common.ingredient.Ingredient;
import io.github.eng_25.piazzapanic.common.interactable.Counter;
import io.github.eng_25.piazzapanic.common.interactable.InteractionFactory;
import io.github.eng_25.piazzapanic.common.interactable.InteractionStation;
import io.github.eng_25.piazzapanic.common.interactable.PantryBox;
import io.github.eng_25.piazzapanic.screen.ScreenGame;
import io.github.eng_25.piazzapanic.util.ResourceManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PiazzaMap {

    public static final float TILE_SIZE = 32f;

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final OrthographicCamera camera;
    private final List<InteractionStation> interactableList;
    private final InteractionStation[] counters; // for customer logic

    public PiazzaMap(ResourceManager rm, OrthographicCamera camera) {
        this.map = rm.gameMap;
        this.camera = camera;
        // setup TiledMap with 1/32 as tiles are 32x32px
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1 / TILE_SIZE);

        // interaction stations
        final PantryBox bunBox = new PantryBox(new Vector2(0, 1), Ingredient.getIngredient("Bun").prepare());
        final PantryBox tomatoBox = new PantryBox(new Vector2(2, 1), Ingredient.getIngredient("Tomato"));
        final PantryBox onionBox = new PantryBox(new Vector2(4, 1), Ingredient.getIngredient("Onion"));
        final PantryBox lettuceBox = new PantryBox(new Vector2(6, 1), Ingredient.getIngredient("Lettuce"));
        final PantryBox meatBox = new PantryBox(new Vector2(8, 1), Ingredient.getIngredient("Meat"));

        final int COUNTER_COUNT = 3;
        final Vector2[] COUNTER_POSITIONS =
                new Vector2[]{new Vector2(9, 11), new Vector2(9, 9), new Vector2(9, 7)};
        final int PAN_COUNT = 2;
        final Vector2[] PAN_POSITIONS =
                new Vector2[]{new Vector2(1, 13), new Vector2(3, 13)};
        final int CHOPPING_COUNT = 2;
        final Vector2[] CHOPPING_POSITIONS =
                new Vector2[]{new Vector2(5, 13), new Vector2(7, 13)};
        final int BIN_COUNT = 1;
        final Vector2[] BIN_POSITIONS =
                new Vector2[]{new Vector2(0, 14)};
        final int PLATING_COUNT = 3;
        final Vector2[] PLATING_POSITIONS =
                new Vector2[]{new Vector2(4, 13), new Vector2(6, 13), new Vector2(8, 13)};

        // sort counters first as they also need their own list
        List<InteractionStation> counterList =
                InteractionFactory.createStations(COUNTER_COUNT, COUNTER_POSITIONS, "counter");
        counters = new Counter[COUNTER_COUNT];
        assert counterList != null;
        counterList.toArray(counters);

        // pantry boxes
        interactableList = Stream.of
                (List.of(bunBox, tomatoBox, onionBox, lettuceBox, meatBox),
                        counterList,
                        InteractionFactory.createStations(PAN_COUNT, PAN_POSITIONS, "pan"),
                        InteractionFactory.createStations(CHOPPING_COUNT, CHOPPING_POSITIONS, "chopping"),
                        InteractionFactory.createStations(BIN_COUNT, BIN_POSITIONS, "bin"),
                        InteractionFactory.createStations(PLATING_COUNT, PLATING_POSITIONS, "plating")
                ).flatMap(Collection::stream).collect(Collectors.toList());

    }

    public void renderMap(SpriteBatch batch, float delta, ScreenGame gameScreen) {
        // render TileMap
        mapRenderer.setView(camera);
        mapRenderer.render();

        // render progress bars
        interactableList.forEach(station -> station.renderProgress(batch, TILE_SIZE));

        // counter and customer logic
        Arrays.stream(counters).forEach(counter -> {
            if (counter instanceof Counter) {
                if (((Counter) counter).hasCustomer()) {
                    ((Counter) counter).getCustomer().tick(delta, batch, gameScreen);
                }
            }
        });
    }

    public InteractionStation checkInteraction(Cook cook) {
        Vector2 cookPos = cook.getPosition();
        InteractionStation nearest = null;
        float nearestDist = 999;
        for (InteractionStation station : interactableList) {
            // find nearest position
            Vector2 pos = station.getPosition();

            float dist = pos.dst(cookPos);
            if (dist < nearestDist && dist <= 1) {
                nearestDist = dist;
                nearest = station;
            }
        }
        return nearest;
    }

    public Counter getFreeCounter() {
        for (Counter counter : (Counter[]) counters) {
            if (!counter.hasCustomer()) {
                return counter;
            }
        }
        return null;
    }

    public void dispose() {
        mapRenderer.dispose();
    }

    public TiledMap getMap() {
        return map;
    }

    public OrthogonalTiledMapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public Counter[] getCounters() {
        return (Counter[]) counters;
    }
}
