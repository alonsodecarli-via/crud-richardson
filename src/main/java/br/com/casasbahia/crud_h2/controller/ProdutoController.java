package br.com.casasbahia.crud_h2.controller;

import br.com.casasbahia.crud_h2.model.Produto;
import br.com.casasbahia.crud_h2.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    public ResponseEntity<Produto> criar(@RequestBody Produto produto) {
        return Optional.of(produto)
                .map(produtoService::criar)
                .map(saved -> URI.create("/api/produtos/" + saved.getId()))
                .map(uri -> ResponseEntity.created(uri).body(produto))
                .orElse(ResponseEntity.badRequest().build());
    }


    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Produto>>> listar() {
        List<EntityModel<Produto>> recursos = produtoService.listar().stream()
                .map(p -> EntityModel.of(p,
                        linkTo(methodOn(ProdutoController.class).buscarPorId(p.getId())).withSelfRel()))
                .collect(Collectors.toList());

        return recursos.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(
                CollectionModel.of(recursos,
                        linkTo(methodOn(ProdutoController.class).listar()).withSelfRel()));
    }


    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Produto>> buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id)
                .map(p -> {
                    // Monta manualmente cada link com URI relativa e type
                    Link self = Link.of("/api/produtos/" + id)
                            .withSelfRel()
                            .withType(HttpMethod.GET.name());
                    Link list = Link.of("/api/produtos")
                            .withRel("produtos")
                            .withType(HttpMethod.GET.name());
                    Link update = Link.of("/api/produtos/" + id)
                            .withRel("atualizar")
                            .withType(HttpMethod.PUT.name());
                    Link delete = Link.of("/api/produtos/" + id)
                            .withRel("deletar")
                            .withType(HttpMethod.DELETE.name());
                    Link comprar = Link.of("/api/produtos/" + id + "/comprar")
                            .withRel("comprar")
                            .withType(HttpMethod.POST.name());

                    return EntityModel.of(p, self, list, update, delete, comprar);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Produto>> atualizar(@PathVariable Long id,
                                                          @RequestBody Produto produto) {
        return produtoService.buscarPorId(id)
                .map(existing -> {
                    produto.setId(id);
                    Produto atualizado = produtoService.atualizar(produto);
                    return EntityModel.of(atualizado,
                            linkTo(methodOn(ProdutoController.class).buscarPorId(id)).withSelfRel(),
                            linkTo(methodOn(ProdutoController.class).listar()).withRel("produtos")
                    );
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<? extends RepresentationModel<?>> deletar(@PathVariable Long id) {
        return produtoService.buscarPorId(id)
                .map(p -> {
                    produtoService.deletar(id);
                    RepresentationModel<?> model = new RepresentationModel<>();
                    model.add(Link.of("/api/produtos")
                            .withRel("produtos")
                            .withType(HttpMethod.GET.name()));
                    return ResponseEntity.ok(model);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }



}
