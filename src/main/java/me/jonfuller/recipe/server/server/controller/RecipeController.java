package me.jonfuller.recipe.server.server.controller;

import me.jonfuller.recipe.api.RecipesApi;
import me.jonfuller.recipe.api.model.Ingredient;
import me.jonfuller.recipe.api.model.Method;
import me.jonfuller.recipe.api.model.Recipe;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import javax.validation.Valid;
import java.util.*;

@RestController
public class RecipeController implements RecipesApi {
//    @Override
//    public Optional<NativeWebRequest> getRequest() {
//        return Optional.empty();
//    }
    private static final Region region = Region.EU_WEST_1;
    private static final String profileName = "default";
    private static final String tableName = "recipes";

    private static DynamoDbClient client = DynamoDbClient.builder()
            .region(region)
            .credentialsProvider(ProfileCredentialsProvider.builder()
                    .profileName(profileName)
                    .build())
            .build();

    @Override
    public ResponseEntity<Void> createRecipes() {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<List<Recipe>> listRecipes(@Valid Integer limit) {

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanIterable response = client.scanPaginator(request);

        ArrayList<Recipe> recipes = new ArrayList<>();

        for (ScanResponse page : response) {
            System.out.println(page.toString());

            for (Map<String, AttributeValue> item : page.items()) {
                System.out.println(item);

                Recipe recipe = queryToRecipe(item);

                recipes.add(recipe);
            }
        }

        return ResponseEntity.ok(recipes);
    }

    /**
     * Map a dynamodb response to a Recipe object.
     * @param item dynamodb recipe item
     * @return a recipe
     */
    private Recipe queryToRecipe(Map<String, AttributeValue> item) {
        Recipe recipe = new Recipe();

        recipe.setId(Long.parseLong(item.get("id").n()));

        recipe.setName(item.get("name").s());

        List<Ingredient> ingredientList = new ArrayList<>();
        for(AttributeValue ing : item.get("ingredients").l()) {
            Ingredient ingredient = new Ingredient();
            ingredient.setId(Long.parseLong(ing.m().get("id").n()));
            ingredient.setName(ing.m().get("name").s());
            ingredient.setAmount(Float.parseFloat(ing.m().get("amount").n()));
            ingredient.setUnit(ing.m().get("unit").s());
            ingredientList.add(ingredient);
        }
        recipe.setIngredients(ingredientList);

        for(AttributeValue met : item.get("method").l()) {
            Method method = new Method();
            method.setId(Long.parseLong(met.m().get("id").n()));
            method.setMethod(met.m().get("method").s());
            method.setTemperature(Integer.parseInt(met.m().get("temperature").n()));
            recipe.setMethod(method);
        }

        List<String> tagList = new ArrayList<>();
        for(AttributeValue ing : item.get("tags").l()) {
            tagList.add(ing.s());
        }
        recipe.setIngredients(ingredientList);

        recipe.setTags(tagList);

        recipe.setImage(item.get("image").s());

        return recipe;
    }

    @Override
    public ResponseEntity<Recipe> showRecipeById(String recipeId) {
        HashMap<String,AttributeValue> keyToGet = new HashMap<>();

        keyToGet.put("id", AttributeValue.builder()
                .n(recipeId).build());

        GetItemRequest req = GetItemRequest.builder()
                .key(keyToGet)
                .tableName("recipes")
                .build();

        Map<String,AttributeValue> returnedItem = client.getItem(req).item();

        return ResponseEntity.ok(queryToRecipe(returnedItem));
    }
}
