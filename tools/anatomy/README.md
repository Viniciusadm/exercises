# Geometria anatômica

`app/src/main/assets/anatomy/body_regions_v1.json` é gerado a partir do projeto
[vulovix/body-muscles](https://github.com/vulovix/body-muscles) (Apache-2.0), no commit
fixado dentro de `build-regions.mjs`.

```
node tools/anatomy/build-regions.mjs
```

A saída é **commitada**: o script é one-off e não faz parte do build do Gradle. A geometria
é asset de build, versionada junto com o código que a desenha — se ela mudasse fora de um
release, o app renderizaria regiões cujos ids o tratamento de toque não conhece.

Para atualizar: troque `SOURCE_COMMIT`, rode o script, e confira que
`AnatomyContractTest` continua verde (ele cruza os ids do asset com os que o backend
manda em `svg_regions`).

A atribuição da licença fica em `app/src/main/assets/anatomy/LICENSE-body-muscles.txt`
e é exibida na tela de licenças do app.
