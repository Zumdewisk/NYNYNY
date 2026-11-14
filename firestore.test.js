const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      displayName: "Alice",
      age: 29,
      heightCm: 172,
      weightKg: 65.0,
      activityLevel: "moderate",
      goalType: "maintain",
      targetCalories: 2100,
      targetProteinG: 130,
      targetCarbsG: 220,
      targetFatG: 65,
      targetWaterMl: 2300,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      targetCalories: 2200,
      targetProteinG: 140,
      targetCarbsG: 220,
      targetFatG: 70,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can add and list own meals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("meals").doc("meal_1").set({
      userId: ALICE_UID,
      date: "2026-11-03",
      mealType: "lunch",
      name: "Kycklingsallad",
      calories: 450,
      protein: 38,
      carbs: 22,
      fat: 14,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("meals").get()
  );
});

test("Authenticated user: Bob cannot read Alice's meals", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("meals").doc("meal_1").set({
      userId: ALICE_UID,
      date: "2026-11-03",
      mealType: "lunch",
      name: "Kycklingsallad",
      calories: 450,
      protein: 38,
      carbs: 22,
      fat: 14,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("meals").doc("meal_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("meals").get());
});

test("Authenticated user: can add and read own weight log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("weight_logs").doc("weight_1").set({
      userId: ALICE_UID,
      logId: "weight_1",
      date: "2026-11-12",
      weightKg: 74.5,
      note: "Morgonvikt fastande",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("weight_logs").get()
  );
});

test("Authenticated user: Bob cannot read Alice's weight logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("weight_logs").doc("weight_1").set({
      userId: ALICE_UID,
      logId: "weight_1",
      date: "2026-11-12",
      weightKg: 74.5,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("weight_logs").doc("weight_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("weight_logs").get());
});

test("Authenticated user: can add and read own workout log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workout_logs").doc("workout_1").set({
      userId: ALICE_UID,
      logId: "workout_1",
      date: "2026-11-13",
      workoutType: "running",
      title: "Löpning",
      durationMinutes: 45,
      caloriesBurned: 480,
      notes: "Kvällsrunda 7 km",
      syncedToHealthConnect: false,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workout_logs").get()
  );
});

test("Authenticated user: Bob cannot read Alice's workout logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("workout_logs").doc("workout_1").set({
      userId: ALICE_UID,
      logId: "workout_1",
      date: "2026-11-13",
      workoutType: "strength",
      title: "Styrketräning",
      durationMinutes: 60,
      caloriesBurned: 380,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workout_logs").doc("workout_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workout_logs").get());
});

test("Authenticated user: can save and list own recipes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("recipe_1").set({
      userId: ALICE_UID,
      recipeId: "recipe_1",
      name: "Proteinpannkakor",
      description: "Snabba pannkakor med kvarg och havregryn",
      servings: 2,
      totalCalories: 640,
      totalProtein: 52.0,
      totalCarbs: 68.0,
      totalFat: 14.0,
      totalFiber: 8.0,
      itemsJson: "[]",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").get()
  );
});

test("Authenticated user: Bob cannot read Alice's recipes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("recipes").doc("recipe_1").set({
      userId: ALICE_UID,
      recipeId: "recipe_1",
      name: "Hemlig gryta",
      servings: 4,
      totalCalories: 1600,
      totalProtein: 120.0,
      totalCarbs: 150.0,
      totalFat: 45.0,
      itemsJson: "[]",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").doc("recipe_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").get());
});

test("Authenticated user: can save and list own AI corrections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_corrections").doc("corr_1").set({
      userId: ALICE_UID,
      correctionId: "corr_1",
      originalDish: "Stekt nötfärs",
      userFeedback: "Det är kycklingfärs, inte nötfärs",
      correctedDish: "Stekt kycklingfärs",
      learningRule: "Användaren äter kycklingfärs istället för nötfärs i vardagsrätter.",
      keywords: "kycklingfärs, nötfärs",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_corrections").get()
  );
});

test("Authenticated user: Bob cannot read or write Alice's AI corrections", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("ai_corrections").doc("corr_1").set({
      userId: ALICE_UID,
      correctionId: "corr_1",
      originalDish: "Filmjölk",
      userFeedback: "Det är grekisk yoghurt 10%",
      correctedDish: "Grekisk yoghurt 10%",
      learningRule: "Vit yoghurt i skål hos användaren är grekisk yoghurt 10%.",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ai_corrections").doc("corr_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ai_corrections").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("ai_corrections").doc("corr_2").set({
      userId: ALICE_UID,
      correctionId: "corr_2",
      originalDish: "A",
      userFeedback: "B",
      correctedDish: "C",
      learningRule: "D",
      createdAt: new Date(),
    })
  );
});


