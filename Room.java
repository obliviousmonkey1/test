package com.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class Room {

    private float fPlayerX = 14.7f;  // Player starting X position
    private float fPlayerY = 5.09f;  // Player starting Y position
    private float fPlayerA = 0.0f;   // Player view angle
    private float fFOV = (float) Math.PI / 2.0f;  // Field of view
    private float fDepth = 16.0f;    // Maximum depth
    private float fSpeed = 6.0f;     // Player movement speed

    private float[] fDepthBuffer;
    private String[] map;             // Create map
    private static int nMapWidth;
    private static int nMapHeight;

    private boolean debug = true;

    // Mini-map properties
    private static int miniMapWidth = 160;  // Mini-map width in pixels
    private static int miniMapHeight = 160; // Mini-map height in pixels
    private static float miniMapScale = 10f; // Scale factor for the mini-map

    // Sprite List
    private List<SObject> spriteList = new ArrayList<>();

    public Room(String[] map) {
        this.map = map;
        nMapWidth = map[0].length();
        nMapHeight = map.length;

        Texture spriteTorch = new Texture(Gdx.files.internal("torch.png"));
        fDepthBuffer = new float[Gdx.graphics.getWidth()];

        // Load sprites based on the map
        loadSprites(spriteTorch);
    }

    private void loadSprites(Texture spriteTorch) {
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length(); x++) {
                if (map[y].charAt(x) == 'T') {
                    spriteList.add(new SObject(x, y, 0.0f, 0.0f, false, spriteTorch));
                }
            }
        }
        // Add additional objects
        spriteList.add(new SObject(4, 9, 0.0f, 0.0f, false, spriteTorch));
    }

    public void setFplayerX(float fplayerX) {
        this.fPlayerX = fplayerX;
    }

    public void setFplayerY(float fplayerY) {
        this.fPlayerY = fplayerY;
    }

    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, boolean gameActive) {
        float deltaTime = Gdx.graphics.getDeltaTime();
        handleInput(deltaTime, gameActive);

        // Clear the screen
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Perform raycasting
        performRaycasting(shapeRenderer);

        for (int i = 0; i < fDepthBuffer.length; i++) {
            fDepthBuffer[i] = Float.MAX_VALUE; // or set to a large enough value
        }

//         Update & Draw Objects
        for (SObject object : this.spriteList){
            renderObject(object, deltaTime, batch);

        }
//        updateAndDrawObjects(batch, deltaTime);

        shapeRenderer.end();

        drawMiniMap(batch, shapeRenderer, font);
    }

    private void handleInput(float deltaTime, boolean gameActive) {
        if (!gameActive) {
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                fPlayerA -= fSpeed * 0.75f * deltaTime;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                fPlayerA += fSpeed * 0.75f * deltaTime;
            }
            // Handle player movement (forward and backward)
            handleMovement(deltaTime);
        }
    }

    private void handleMovement(float deltaTime) {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            movePlayer(deltaTime, true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            movePlayer(deltaTime, false);
        }
    }

    private void movePlayer(float deltaTime, boolean isForward) {
        float moveDirection = isForward ? 1 : -1;
        fPlayerX += Math.sin(fPlayerA) * fSpeed * moveDirection * deltaTime;
        fPlayerY += Math.cos(fPlayerA) * fSpeed * moveDirection * deltaTime;

        // Collision detection with walls
        if (map[(int) fPlayerY].charAt((int) fPlayerX) == '#') {
            fPlayerX -= Math.sin(fPlayerA) * fSpeed * moveDirection * deltaTime;
            fPlayerY -= Math.cos(fPlayerA) * fSpeed * moveDirection * deltaTime;
        }
    }

    private void performRaycasting(ShapeRenderer shapeRenderer) {
        for (int x = 0; x < Gdx.graphics.getWidth(); x++) {
            float fRayAngle = (fPlayerA - fFOV / 2.0f) + ((float) x / Gdx.graphics.getWidth()) * fFOV;
            float fDistanceToWall = castRay(fRayAngle);

            // Calculate ceiling and floor
            drawColumns(shapeRenderer, x, fDistanceToWall);
        }
    }

    private float castRay(float fRayAngle) {
        float fStepSize = 0.1f;
        float fDistanceToWall = 0.0f;
        boolean bHitWall = false;

        float fEyeX = (float) Math.sin(fRayAngle);
        float fEyeY = (float) Math.cos(fRayAngle);

        while (!bHitWall && fDistanceToWall < fDepth) {
            fDistanceToWall += fStepSize;

            int nTestX = (int) (fPlayerX + fEyeX * fDistanceToWall);
            int nTestY = (int) (fPlayerY + fEyeY * fDistanceToWall);

            // Check bounds
            if (nTestX < 0 || nTestX >= nMapWidth || nTestY < 0 || nTestY >= nMapHeight) {
                bHitWall = true; // Set distance to maximum depth
                fDistanceToWall = fDepth;
            } else {
                if (map[nTestY].charAt(nTestX) == '#') {
                    bHitWall = true;
                }
            }
        }
        return fDistanceToWall;
    }

    private void drawColumns(ShapeRenderer shapeRenderer, int x, float fDistanceToWall) {
        int nCeiling = (int) ((Gdx.graphics.getHeight() / 2.0f) - (Gdx.graphics.getHeight() / (fDistanceToWall + 0.0001f)));
        int nFloor = Gdx.graphics.getHeight() - nCeiling;

        // Ensure nCeiling and nFloor are within bounds
        nCeiling = Math.max(0, Math.min(nCeiling, Gdx.graphics.getHeight() - 1));
        nFloor = Math.max(0, Math.min(nFloor, Gdx.graphics.getHeight() - 1));

        for (int y = 0; y < Gdx.graphics.getHeight(); y++) {
            if (y <= nCeiling) {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1); // Ceiling color
            } else if (y > nCeiling && y <= nFloor) {
                float wallShade = 1.0f / (fDistanceToWall + 0.0001f); // Prevent division by zero
                shapeRenderer.setColor(wallShade, wallShade, wallShade, 1); // Wall color
            } else {
                shapeRenderer.setColor(0.0f, 0.5f, 0.0f, 1); // Floor color
            }
            shapeRenderer.rect(x, y, 1, 1); // Draw rectangle
        }
    }

    private void renderObject(SObject object, float deltaTime, SpriteBatch batch) {
        // Update Object Physics
        object.x += object.vx * deltaTime;
        object.y += object.vy * deltaTime;

        // Check if object is inside wall - set flag for removal
        if (map[(int) object.y].charAt((int) object.x) == '#') {
            object.bRemove = true;
            return; // Exit early if object should be removed
        }

        // Calculate distance and angle
        float fVecX = object.x - fPlayerX;
        float fVecY = object.y - fPlayerY;
        float fDistanceFromPlayer = (float)Math.sqrt(fVecX * fVecX + fVecY * fVecY);

        float fEyeX = (float)Math.sin(fPlayerA);
        float fEyeY = (float)Math.cos(fPlayerA);
        float fObjectAngle = (float)Math.atan2(fEyeY, fEyeX) - (float)Math.atan2(fVecY, fVecX);

        // Normalize angle
        if (fObjectAngle < -Math.PI) fObjectAngle += 2.0f * Math.PI;
        if (fObjectAngle > Math.PI) fObjectAngle -= 2.0f * Math.PI;

        boolean bInPlayerFOV = Math.abs(fObjectAngle) < fFOV / 2.0f;

        // Check visibility and render if applicable
        if (bInPlayerFOV && fDistanceFromPlayer >= 0.5f && fDistanceFromPlayer < fDepth && !object.bRemove) {
            float fObjectCeiling = (Gdx.graphics.getHeight() / 2.0f) - (Gdx.graphics.getHeight() / fDistanceFromPlayer);
            float fObjectFloor = Gdx.graphics.getHeight() - fObjectCeiling;
            float fObjectHeight = fObjectFloor - fObjectCeiling;
            float fObjectAspectRatio = (float)object.sprite.getHeight() / object.sprite.getWidth();
            float fObjectWidth = fObjectHeight / fObjectAspectRatio;
            float fMiddleOfObject = (0.5f * (fObjectAngle / (fFOV / 2.0f)) + 0.5f) * Gdx.graphics.getHeight();

            drawObjectSprite(object, fObjectWidth, fObjectHeight, fObjectCeiling, fMiddleOfObject, fDistanceFromPlayer, batch);
        }
        spriteList.removeIf(o -> o.bRemove);

    }

    private void drawObjectSprite(SObject object, float fObjectWidth, float fObjectHeight, float fObjectCeiling, float fMiddleOfObject, float fDistanceFromPlayer, SpriteBatch batch) {
        for (float lx = 0; lx < fObjectWidth; lx++) {
            for (float ly = 0; ly < fObjectHeight; ly++) {
                float fSampleX = lx / fObjectWidth;
                float fSampleY = ly / fObjectHeight;
                char c = object.sampleGlyph(fSampleX, fSampleY);
                int nObjectColumn = (int)(fMiddleOfObject + lx - (fObjectWidth / 2.0f));

                if (nObjectColumn >= 0 && nObjectColumn < Gdx.graphics.getWidth()) {
                    if (c != ' ' && fDepthBuffer[nObjectColumn] >= fDistanceFromPlayer) {
                        batch.begin();
                        batch.draw(object.sprite, nObjectColumn, fObjectCeiling + ly);
                        batch.end();
                        fDepthBuffer[nObjectColumn] = fDistanceFromPlayer;
                    }
                }
            }
        }
    }


    private void updateAndDrawObjects(SpriteBatch batch, float deltaTime) {

//


        for (SObject object : this.spriteList) {
            // Update Object Physics
            object.x += object.vx * deltaTime;
            object.y += object.vy * deltaTime;

            // Check if object is inside wall - set flag for removal
            if (map[(int) object.y].charAt((int) object.x) == '#') {
                object.bRemove = true;
            }

            // Can object be seen?
            float fVecX = object.x - fPlayerX;
            float fVecY = object.y - fPlayerY;
            float fDistanceFromPlayer = (float) Math.sqrt(fVecX * fVecX + fVecY * fVecY);

            float fEyeX = (float) Math.sin(fPlayerA);
            float fEyeY = (float) Math.cos(fPlayerA);

            // Calculate angle between lamp and players feet, and players looking direction
            float fObjectAngle = (float) Math.atan2(fEyeY, fEyeX) - (float) Math.atan2(fVecY, fVecX);
            if (fObjectAngle < -Math.PI) fObjectAngle += 2.0f * Math.PI;
            if (fObjectAngle > Math.PI) fObjectAngle -= 2.0f * Math.PI;

            boolean bInPlayerFOV = Math.abs(fObjectAngle) < fFOV / 2.0f;

            if (bInPlayerFOV && fDistanceFromPlayer >= 0.5f && fDistanceFromPlayer < fDepth && !object.bRemove) {
                float fObjectCeiling = (Gdx.graphics.getHeight() / 2.0f) - (Gdx.graphics.getHeight() / fDistanceFromPlayer);
                float fObjectFloor = Gdx.graphics.getHeight() - fObjectCeiling;
                float fObjectHeight = fObjectFloor - fObjectCeiling;
                float fObjectAspectRatio = (float) object.sprite.getHeight() / (float) object.sprite.getWidth();
                float fObjectWidth = fObjectHeight / fObjectAspectRatio;
                float fMiddleOfObject = (0.5f * (fObjectAngle / (fFOV / 2.0f)) + 0.5f) * Gdx.graphics.getHeight();

                // Draw Lamp
                for (float lx = 0; lx < fObjectWidth; lx++) {
                    for (float ly = 0; ly < fObjectHeight; ly++) {
                        float fSampleX = lx / fObjectWidth;
                        float fSampleY = ly / fObjectHeight;
                        char c = object.sampleGlyph(fSampleX, fSampleY);

                        int nObjectColumn = (int) (fMiddleOfObject + lx - (fObjectWidth / 2.0f));

                        if (nObjectColumn >= 0 && nObjectColumn < Gdx.graphics.getWidth()) {
                            if (c != ' ' && fDepthBuffer[nObjectColumn] >= fDistanceFromPlayer) {
                                batch.begin();
                                batch.draw(object.sprite, nObjectColumn, fObjectCeiling + ly);
                                batch.end();

                                fDepthBuffer[nObjectColumn] = fDistanceFromPlayer;
                            }
                        }
                    }
                }
            }
        }

// Remove objects marked for removal after rendering
        spriteList.removeIf(o -> o.bRemove);

    }

    private void drawMiniMap(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw mini-map background
        shapeRenderer.setColor(0, 0, 0, 0.5f); // Mini-map background color
        shapeRenderer.rect(0, Gdx.graphics.getHeight() - miniMapHeight, miniMapWidth, miniMapHeight); // Background rectangle

        // Draw map
        for (int y = 0; y < nMapHeight; y++) {
            for (int x = 0; x < nMapWidth; x++) {
                char tile = map[y].charAt(x);
                if (tile == '#') {
                    shapeRenderer.setColor(1, 1, 1, 1); // Wall color
                } else {
                    shapeRenderer.setColor(0, 0, 0, 0); // Empty space color
                }
                // Draw each tile on the mini-map
                shapeRenderer.rect(x * miniMapScale, Gdx.graphics.getHeight() - miniMapHeight + (nMapHeight - y - 1) * miniMapScale, miniMapScale, miniMapScale);
            }
        }

        // Draw player position on the mini-map
        shapeRenderer.setColor(0, 1, 0, 1); // Player color
        shapeRenderer.rect(fPlayerX * miniMapScale, Gdx.graphics.getHeight() - miniMapHeight + (nMapHeight - (int) fPlayerY - 1) * miniMapScale, miniMapScale / 2, miniMapScale / 2); // Player representation

        // Draw sprites on the mini-map
        for (SObject sprite : spriteList) {
            shapeRenderer.setColor(1, 0, 0, 1); // Sprite color (red for visibility)
            shapeRenderer.rect(sprite.x * miniMapScale, Gdx.graphics.getHeight() - miniMapHeight + (nMapHeight - (int) sprite.y - 1) * miniMapScale, miniMapScale / 2, miniMapScale / 2); // Sprite representation
        }


        if(debug){
            // Draw rays on the mini-map
            shapeRenderer.setColor(1, 1, 0, 1); // Ray color (yellow for visibility)

            for (int x = 0; x < Gdx.graphics.getWidth(); x++) {
                float fRayAngle = (fPlayerA - fFOV / 2.0f) + ((float) x / (float) Gdx.graphics.getWidth()) * fFOV;

                float fStepSize = 0.1f;
                float fDistanceToWall = 0.0f;

                boolean bHitWall = false;

                float fEyeX = (float) Math.sin(fRayAngle);
                float fEyeY = (float) Math.cos(fRayAngle);

                // Incrementally cast ray from player, along ray angle, testing for intersection with a block
                while (!bHitWall && fDistanceToWall < fDepth) {
                    fDistanceToWall += fStepSize;

                    int nTestX = (int) (fPlayerX + fEyeX * fDistanceToWall);
                    int nTestY = (int) (fPlayerY + fEyeY * fDistanceToWall);

                    // Check if the ray is out of bounds
                    if (nTestX < 0 || nTestX >= nMapWidth || nTestY < 0 || nTestY >= nMapHeight) {
                        bHitWall = true; // Set distance to maximum depth
                        fDistanceToWall = fDepth;
                    } else {
                        // Check if the ray has hit a wall
                        if (map[nTestY].charAt(nTestX) == '#') {
                            bHitWall = true;
                        }
                    }
                }

                // Calculate ray endpoint for drawing on mini-map
                float rayEndX = fPlayerX + fEyeX * fDistanceToWall;
                float rayEndY = fPlayerY + fEyeY * fDistanceToWall;

                // Draw the ray on the mini-map
                shapeRenderer.line(
                    fPlayerX * miniMapScale,
                    Gdx.graphics.getHeight() - miniMapHeight + (nMapHeight - (int) fPlayerY - 1) * miniMapScale,
                    rayEndX * miniMapScale,
                    Gdx.graphics.getHeight() - miniMapHeight + (nMapHeight - (int) rayEndY - 1) * miniMapScale
                );
            }
        }


        shapeRenderer.end();

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();

        // Format memory usage in MB
        float usedMemoryMB = usedMemory / (1024f * 1024f);
        float totalMemoryMB = totalMemory / (1024f * 1024f);
        float maxMemoryMB = maxMemory / (1024f * 1024f);

        batch.begin(); // Begin batch rendering
        font.setColor(1, 1, 1, 1); // Set font color to white
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10); // Draw FPS at top-left corner of mini-map
        if(debug){
            // Draw memory usage text
            font.draw(batch, "Used Memory: " + String.format("%.2f MB", usedMemoryMB), 10, Gdx.graphics.getHeight() - 30);
            font.draw(batch, "Total Memory: " + String.format("%.2f MB", totalMemoryMB), 10, Gdx.graphics.getHeight() - 50);
            font.draw(batch, "Max Memory: " + String.format("%.2f MB", maxMemoryMB), 10, Gdx.graphics.getHeight() - 70);
        }
        batch.end(); // End batch rendering

    }


}

class SObject {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public boolean bRemove;
    public Texture sprite;

    // Constructor
    public SObject(float x, float y, float vx, float vy, boolean bRemove, Texture sprite) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.bRemove = bRemove;
        this.sprite = sprite;
    }

    public char sampleGlyph(float sampleX, float sampleY) {
        // Replace this with your actual glyph sampling logic
        // For demonstration, we'll return a character based on coordinates
        return 'A'; // Placeholder character
    }
}
