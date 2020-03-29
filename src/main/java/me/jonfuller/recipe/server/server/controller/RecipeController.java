package me.jonfuller.recipe.server.server.controller;

import me.jonfuller.recipe.api.RecipesApi;
import me.jonfuller.recipe.api.model.Recipe;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class RecipeController implements RecipesApi {
//    @Override
//    public Optional<NativeWebRequest> getRequest() {
//        return Optional.empty();
//    }

    @Override
    public ResponseEntity<Void> createRecipes() {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<List<Recipe>> listRecipes(@Valid Integer limit) {
        Recipe rec1 = new Recipe();
        rec1.setId(1L);
        rec1.setName("custard pie");
        rec1.setTag("dessert");

        Recipe rec2 = new Recipe();
        rec2.setId(2L);
        rec2.setName("spaghetti bolognese");
        rec2.setTag("main");

        return ResponseEntity.ok(List.of(rec1, rec2));
    }

    @Override
    public ResponseEntity<Recipe> showRecipeById(String recipeId) {
        Recipe rec = new Recipe();
        rec.setId(2L);
        rec.setName("spaghetti bolognese");
        rec.setTag("main");

        return ResponseEntity.ok(rec);
    }
}
