package me.jonfuller.recipe.server.server.controller;

import me.jonfuller.recipe.api.RecipesApi;
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
    private final Region region = Region.EU_WEST_1;
    private final String profileName = "default";
    private final String tableName = "recipes";

    @Override
    public ResponseEntity<Void> createRecipes() {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<List<Recipe>> listRecipes(@Valid Integer limit) {
        DynamoDbClient client = DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.builder()
                        .profileName(profileName)
                        .build())
                .build();

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

                recipes.add(recipe);
            }
        }

//        Recipe rec1 = new Recipe();
//        rec1.setId(1L);
//        rec1.setName("custard pie");
//        rec1.setTags(List.of("dessert", "tasty"));
//
//        Recipe rec2 = new Recipe();
//        rec2.setId(2L);
//        rec2.setName("spaghetti bolognese");
//        rec2.setTags(List.of("main"));

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
