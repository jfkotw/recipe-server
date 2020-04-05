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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.services.dynamodb.transform.BatchGetItemRequestMarshaller;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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


//        BatchGetItemRequest batchReq = BatchGetItemRequest.builder()
//                .requestItems()
//                .build();

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanIterable response = client.scanPaginator(request);

        ArrayList<Recipe> recipes = new ArrayList<>();


        for (ScanResponse page : response) {
            System.out.println(page.toString());

            for (Map<String, AttributeValue> item : page.items()) {
                System.out.println(item);

                // Map response to Recipe object.
                Recipe recipe = new Recipe();
                recipe.setId(Long.parseLong(item.get("id").n()));
                recipe.setName(item.get("name").s());

                List<Ingredient> ingredientList = new ArrayList<>();
                for(AttributeValue ing : item.get("ingredients").l()) {
//                    System.out.println(ing.toString());
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(Long.parseLong(ing.m().get("id").n()));
                    ingredient.setName(ing.m().get("name").s());
                    ingredient.setAmount(Float.parseFloat(ing.m().get("amount").n()));
                    ingredient.setUnit(ing.m().get("unit").s());
                    ingredientList.add(ingredient);
                }
                recipe.setIngredients(ingredientList);

//                List<Method> methodList = new ArrayList<>();
                for(AttributeValue met : item.get("method").l()) {
//                    System.out.println(ing.toString());
                    Method method = new Method();
                    method.setId(Long.parseLong(met.m().get("id").n()));
                    method.setMethod(met.m().get("method").s());
                    method.setTemperature(Integer.parseInt(met.m().get("temperature").n()));
                    recipe.setMethod(method);
//                    methodList.add(method);
                }

                List<String> tagList = new ArrayList<>();
                for(AttributeValue ing : item.get("tags").l()) {
//                    System.out.println(ing.toString());
                    tagList.add(ing.s());

                }
                recipe.setIngredients(ingredientList);
                recipe.setTags(tagList);
                recipe.setImage(item.get("image").s());

                recipes.add(recipe);
            }
        }

        return ResponseEntity.ok(recipes);
    }

    @Override
    public ResponseEntity<Recipe> showRecipeById(String recipeId) {
        Recipe rec = new Recipe();
        rec.setId(2L);
        rec.setName("spaghetti bolognese");
        rec.setTags(List.of("main"));

        return ResponseEntity.ok(rec);
    }
}
