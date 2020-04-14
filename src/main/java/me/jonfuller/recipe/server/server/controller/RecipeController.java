package me.jonfuller.recipe.server.server.controller;

import me.jonfuller.recipe.api.RecipesApi;
import me.jonfuller.recipe.api.model.Ingredient;
import me.jonfuller.recipe.api.model.Method;
import me.jonfuller.recipe.api.model.Recipe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URL;
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
    private static final String imageBucket = "recipes.jonfuller.me";
    private static final String imageBucketHost = "s3-eu-west-1.amazonaws.com";

    private static DynamoDbClient client = DynamoDbClient.builder()
            .region(region)
            .credentialsProvider(ProfileCredentialsProvider.builder()
                    .profileName(profileName)
                    .build())
            .build();

    @Override
    public ResponseEntity<Void> createRecipes(Recipe recipe) {
        HashMap<String,AttributeValue> itemValues;

        // Add content to the table
        itemValues = recipeToQuery(recipe);

        // Create a PutItemRequest object
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build();

        try {
            client.putItem(request);
            System.out.println(tableName +" was successfully updated");
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (ResourceNotFoundException e) {
            System.err.format("Error: The table \"%s\" can't be found.\n", tableName);
            System.err.println("Be sure that it exists and that you've typed its name correctly!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
        }

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
     * Map a Recipe to a dynamodb item
     *
     */
    private HashMap<String, AttributeValue> recipeToQuery(Recipe recipe) {
        HashMap<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder().n(recipe.getId().toString()).build());

        item.put("name", AttributeValue.builder().s(recipe.getName()).build());

        //ingredients
        //TODO: Implement ingredients.
        //method
        HashMap<String, AttributeValue> methodMap = new HashMap<>();
        methodMap.put("id", AttributeValue.builder().n(recipe.getMethod().getId().toString()).build());
        methodMap.put("method", AttributeValue.builder().s(recipe.getMethod().getMethod()).build());
        methodMap.put("temperature", AttributeValue.builder().n(recipe.getMethod().getTemperature().toString()).build());
        item.put("method", AttributeValue.builder().m(methodMap).build());
//        List<String> tagList = new ArrayList<>();
//        for(AttributeValue ing : item.get("tags").l()) {
//            tagList.add(ing.s());
//        }

        // TODO: Implement so it will add all tags.
//        List<AttributeValue> tagList = new ArrayList<>();
//
//        for (String tag : recipe.getTags()) {
//            tagList.add(AttributeValue.builder("tags", tag).build());
//        }
//        item.put("tags", AttributeValue.builder().l(AttributeValue.builder().l(tagMap).build()).build());

        item.put("image", AttributeValue.builder().s(recipe.getImageKey()).build());

        return item;
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
        recipe.setTags(tagList);

        recipe.setImageKey(item.get("image").s());

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

        try {
            Map<String,AttributeValue> returnedItem = client.getItem(req).item();
            //return ResponseStatus(HttpStatus.MOVED_PERMANENTLY, "https://s3.eu-west-1.amazonaws.com/recipes.jonfuller.me/recipe-api.yml?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20200413T202629Z&X-Amz-SignedHeaders=host&X-Amz-Expires=600&X-Amz-Credential=AKIAQFGBK5E5QSC47DV3%2F20200413%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Signature=22482dd98518d4c66e4e5c1bc97a8de5e615f4e83b4719a85c382f4d2c3c13c2");
            if(returnedItem != null) {
                return ResponseEntity.ok(queryToRecipe(returnedItem));

            } else {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No recipe with ID: "+recipeId
                );
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Ooops"
            );
        }
    }

    @Override
    public ResponseEntity<Void> showRecipeImage(String recipeId) {
        HashMap<String,AttributeValue> keyToGet = new HashMap<>();

        keyToGet.put("id", AttributeValue.builder()
                .n(recipeId).build());

        GetItemRequest req = GetItemRequest.builder()
                .key(keyToGet)
                .tableName("recipes")
                .build();

        try {
            Map<String, AttributeValue> returnedItem = client.getItem(req).item();
            if(returnedItem != null) {
                Recipe recipe = queryToRecipe(returnedItem);
                UriComponents imageFileLocation = UriComponentsBuilder.newInstance()
                        .scheme("https").host(imageBucketHost).pathSegment(imageBucket, recipe.getImageKey()).build();

                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", imageFileLocation.toUriString());
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No image for recipe with ID: "+recipeId
                );
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Ooops"
            );
        }

    }
}
