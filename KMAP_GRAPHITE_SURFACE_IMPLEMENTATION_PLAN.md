# Plano de implementação do GraphiteSurface no KMaP

Este documento especifica a integração do GraphiteSurface no KMaP. A primeira
entrega migra somente mapas vector-only que o GraphiteSurface consegue desenhar
com paridade. Todo mapa que possui `rasterCanvas` continua inteiro no renderer
Compose atual.

O agente deve executar as fases na ordem e respeitar os gates. Não tente
intercalar um `SurfaceView`, `UIKitView`, `SwingPanel` ou elemento HTML nativo
entre raster Compose e outros componentes do mapa. A escolha do renderer é feita
uma vez para o mapa completo.

## Baseline

O plano foi escrito contra estas revisões:

- KMaP: `3e34d792ab15029bf5a2579c938f5d4e5b57619a`;
- GraphiteSurface: `c70f010e7744f57f416280f6e567af6c6064842d`;
- fork do Skiko: `e1fe36dff0a4fd3c293bd7b60cd4ef3b00ad1f01`.

Antes da primeira edição, leia o `AGENTS.md` do KMaP e confirme o estado destes
arquivos:

- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/KMaP.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/MapState.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/components/KMaPContent.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/components/ComponentProvider.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/components/ComponentMeasurePolicy.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/mapSource/tiled/engine/CanvasKernel.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/mapSource/tiled/engine/CanvasEngine.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/mapSource/tiled/canvas/VectorTileCanvas.kt`;
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/utils/style/`;
- `KMaP/build.gradle.kts` e `gradle/libs.versions.toml`;
- `DemoApp` de Android, iOS, Desktop e Web.

Se a revisão do KMaP mudou, preserve as decisões e os testes deste documento.
Adapte os nomes ao código atual. Não restaure código antigo para fazer o plano
encaixar.

## Decisão central

O KMaP escolhe um renderer completo por instância:

```text
KMaPContent
    |
    +-- contém rasterCanvas ------------------------> Compose
    |
    +-- contém marker, cluster ou path -------------> Compose na primeira entrega
    |
    +-- contém vectorCanvas incompatível -----------> Compose
    |
    +-- somente vectorCanvas compatível
          |
          +-- plataforma Graphite aprovada ---------> GraphiteSurface
          |
          +-- plataforma não aprovada --------------> Compose
```

Esta regra é intencional:

- `RasterTileCanvas` não muda;
- o GraphiteSurface não precisa de `drawImageRect`;
- não existe uma surface nativa por canvas;
- não existe uma camada Compose de raster por baixo de Graphite;
- não existe overlay Compose por cima de Graphite antes dos testes específicos
  de cada plataforma;
- `Auto` sempre tem um fallback que produz o mapa completo.

Mesmo que uma plataforma consiga sobrepor Compose a uma surface nativa em um
caso isolado, não use isso na primeira entrega. Android `SurfaceView`, Desktop
Swing/AWT, UIKit e DOM possuem regras diferentes de composição e input. A
integração inicial deve ter o mesmo contrato em todos os targets.

## Resultado esperado

Ao terminar a primeira entrega:

- a API atual `rasterCanvas`, `vectorCanvas`, `marker`, `cluster` e `path`
  continua válida;
- qualquer mapa com pelo menos um `RasterCanvasParameters` usa o caminho
  Compose inteiro;
- mapas vector-only compatíveis usam uma única `GraphiteSurface`;
- o Graphite preserva ordem dos canvases, ordem das style layers, clipping,
  transforms e parent/child tile fallback;
- dois `GraphiteRecorder`s recebem lotes independentes quando o frame possui
  trabalho suficiente;
- o owner do `GraphiteEngine` insere as recordings na ordem visual correta;
- `MapState`, coordenadas, tile loading e gestos mantêm o contrato atual;
- uma falha do Graphite em `Auto` troca a instância para Compose;
- targets sem host Graphite continuam compilando e usando Compose;
- sair da composição fecha o `GraphiteEngine` e cancela o trabalho do renderer;
- filas, jobs e caches permanecem limitados durante pan e zoom.

Texto e símbolos não fazem parte da primeira entrega. Um vector style com
layer `symbol` permanece no Compose até o GraphiteSurface possuir um comando de
texto com paridade suficiente.

## Arquitetura atual

Hoje cada componente ocupa um item de tamanho completo do `LazyLayout`:

```text
KMaP LazyLayout
    |
    +-- RasterTileCanvas, Compose Canvas
    +-- VectorTileCanvas, Compose Canvas
    +-- Path, Compose drawBehind
    +-- Marker e Cluster, composables

MapState.cameraState
    -> CanvasKernel.resolveVisibleTiles()
    -> CanvasEngine.renderTiles()
    -> TileRenderer
    -> ActiveTiles mutableState
    -> recomposição e desenho Compose
```

`CanvasKernel` já decide quais tiles finais, parents e children ficam ativos.
O renderer Graphite deve consumir `CanvasKernel.getActiveTiles(id)`. Não copie
essa seleção para o renderer novo.

## Arquitetura proposta

`KMaP` avalia o DSL uma vez por composição e escolhe uma das duas árvores:

```text
KMaP
    |
    +-- ComposeMap
    |     +-- LazyLayout atual, sem mudança de comportamento
    |
    +-- GraphiteVectorMap
          +-- uma GraphiteSurface
          +-- GraphiteMapController
          +-- GraphiteMapScene imutável
          +-- lotes ordenados por canvas e style layer
          +-- Recorder 0 e Recorder 1
          +-- createFrame, insert em ordem, present
```

Não extraia um `LegacyCanvasPlane`, `OverlayProvider` ou outra duplicação do
layout atual. O branch Compose deve continuar chamando o mesmo
`ComponentProvider`, `ComponentMeasurePolicy` e `LazyLayout` que já existem.
Essa é a parte mais importante da decisão de manter raster no Compose.

## API pública

Adicione um único controle público:

```kotlin
enum class MapRenderBackend {
    Auto,
    Compose,
    Graphite,
}
```

Adicione o parâmetro antes do trailing lambda:

```kotlin
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    renderBackend: MapRenderBackend = MapRenderBackend.Auto,
    content: KMaPContent.() -> Unit,
)
```

Semântica:

- `Auto` usa Graphite somente quando conteúdo e plataforma passam todos os
  gates;
- `Compose` executa sempre o renderer atual;
- `Graphite` exige compatibilidade e lança `IllegalStateException` com a
  primeira incompatibilidade encontrada;
- se o conteúdo mudar durante a composição, `Auto` pode mudar de renderer sem
  recriar `MapState`;
- uma falha assíncrona de device ou apresentação em `Auto` fecha o engine e
  fixa Compose para aquela instância de `KMaP`;
- `Graphite` não esconde falha de runtime com troca automática.

Não exponha `GraphiteEngine`, recorders, recordings ou diagnostics na API
pública. O KMaP é o owner desses recursos.

## Análise de compatibilidade

Crie uma função interna direta em `commonMain`:

```kotlin
internal fun KMaPContent.graphiteIncompatibility(): String?
```

Ela retorna `null` quando o conteúdo pode usar Graphite. Caso contrário retorna
uma mensagem estável, curta e testável. Verifique nesta ordem:

1. existe pelo menos um canvas;
2. não existe `RasterCanvasParameters`;
3. todos os canvases são `VectorCanvasParameters`;
4. não existem markers;
5. não existem clusters;
6. não existem paths;
7. todos os canvases têm `alpha == 1f`;
8. nenhuma style layer usa `symbol`;
9. toda style layer usa um tipo suportado: `background`, `fill` ou `line`;
10. as propriedades de cada layer cabem na implementação Graphite atual;

`graphiteIncompatibility()` verifica somente os itens 1 a 10. A resolução de
backend verifica em seguida se a plataforma possui host aprovado e, no browser,
se WebGPU e os assets do runtime estão disponíveis.

Mensagens sugeridas:

```text
"Graphite does not render maps containing rasterCanvas"
"Graphite does not render markers yet"
"Graphite does not render vector symbol layers yet: {layerId}"
"Graphite requires vector canvas alpha == 1: {canvasId}"
"Graphite is unavailable on {platform}"
```

Uma string basta. Não crie sealed class, lista de capabilities ou registry de
plugins para representar cinco decisões locais.

### Por que `alpha == 1f`

O Compose aplica `CanvasParameters.alpha` ao resultado completo do item via
layer. Multiplicar alpha em cada paint Graphite não produz o mesmo pixel quando
geometrias se sobrepõem. Até o GraphiteSurface expor `saveLayer` com alpha, o
modo Graphite aceita somente canvas opaco.

### Conteúdo dinâmico

`KMaPContent` pode mudar após recomposição. A seleção deve usar o conteúdo da
composição atual:

- `Auto`: ao aparecer raster, marker, path ou symbol layer, descarte o host
  Graphite e renderize o mapa completo no Compose;
- `Auto`: ao voltar para conteúdo compatível, pode recriar Graphite;
- `Graphite`: reporte a incompatibilidade imediatamente;
- `Compose`: não execute análise nem inicialize Graphite.

Não mantenha dois renderers vivos para tornar a troca mais rápida.

## Suporte inicial por plataforma

| Target KMaP | `Auto` inicial | Condição |
| --- | --- | --- |
| Android | Graphite para vector-only compatível | host e teste visual do `SurfaceView` aprovados |
| iOS Arm64 | Graphite para vector-only compatível | Metal e lifecycle aprovados em device |
| iOS Simulator Arm64 | Graphite para vector-only compatível | mesmo teste no simulator |
| JVM macOS | Compose | host JVM e interop AWT ainda precisam de gate próprio |
| JVM Linux | Compose | interop Swing/OpenGL continua fora da primeira entrega |
| JVM Windows | Compose | GraphiteSurface não possui backend JVM Windows |
| JS browser | Graphite para vector-only compatível | WebGPU e staging dos assets aprovados |
| Wasm browser | Graphite para vector-only compatível | WebGPU e staging dos assets aprovados |
| JS Node | Compose | não existe host visual |
| Wasm Node e d8 | Compose | não existe host visual |
| macOS Arm64 nativo | Compose | GraphiteSurface não publica esse target |

Comece com `Auto` retornando Compose em todos os targets. Ative um target
somente depois dos testes de integração e comparação visual dele passarem.

O Web pode usar o worker de comandos atual. Esta integração não depende da
migração pthread descrita em `WEB_PTHREADS_MIGRATION.md`.

## Pré-requisito mínimo no GraphiteSurface

Não existe mais pré-requisito de imagem. Não implemente `drawImageRect` como
parte desta migração. Se essa API já existir por outro motivo, ela pode ficar,
mas o KMaP não a usa neste trabalho.

Para preservar as lines do `VectorTileCanvas`, amplie o stroke atual do
GraphiteSurface somente se ele ainda não expressar cap e join:

```kotlin
data class Stroke(
    val width: Float,
    val cap: StrokeCap = StrokeCap.Butt,
    val join: StrokeJoin = StrokeJoin.Miter,
    val miter: Float = 4f,
) : GraphiteDrawStyle
```

Use os tipos Compose existentes se o módulo já depende deles. Caso o engine
não possa depender de Compose, use diretamente os tipos Skia que o encoder já
aceita. Não crie enums Graphite que apenas renomeiam `StrokeCap` e
`StrokeJoin`.

O comando serializado precisa carregar width, cap, join e miter. Atualize todos
os executores nativos e Web. Teste o round trip do protocolo e pelo menos uma
imagem de referência para cada cap e join.

Se essa alteração ainda não existir, ela é o único bloqueio de desenho da
primeira entrega. Como alternativa temporária, o agente pode aceitar somente
styles cujo cap é `butt` e join é `miter`, mas não deve ativar `Auto` para uma
style que pede outra forma.

## Distribuição do GraphiteSurface

### Compatibilidade de versões

Na baseline, os projetos usam Kotlin `2.4.10`, AGP `9.3.1`, coroutines
`1.11.0`, JVM 17, compile SDK 37 e min SDK 24. O Compose declarado difere:

| Projeto | Compose | Skiko relevante |
| --- | --- | --- |
| KMaP | `1.11.1` | versão transitiva dessa linha |
| GraphiteSurface adapter | `1.12.0-beta03` | o consumer pode selecionar outra linha |
| Graphite engine | sem Compose UI | fork baseado em `0.152.0-alpha01` |

Antes de escrever o renderer:

1. inspecione as classpaths JVM, Android e metadata KMP;
2. confirme que uma única versão de cada módulo Compose chega ao app;
3. confirme que o fork Skiko fornece os símbolos consumidos pelo engine;
4. execute um sample, pois resolução Gradle não prova ABI nativa;
5. no Web, confirme que o stock Skiko não substitui os binários do fork.

### Composite build local

O KMaP pode consumir o checkout irmão. Não grave caminho absoluto.

Em `settings.gradle.kts`:

```kotlin
providers.gradleProperty("graphiteSurfacePath").orNull?.let { path ->
    includeBuild(path) {
        dependencySubstitution {
            substitute(module("com.rafambn:graphite-surface"))
                .using(project(":graphite-surface"))
        }
    }
}
```

No version catalog:

```toml
[versions]
graphite-surface = "0.1.0-SNAPSHOT"

[libraries]
graphite-surface = {
  module = "com.rafambn:graphite-surface",
  version.ref = "graphite-surface"
}
```

Valide o composite antes de mudar pixels:

```shell
rtk ./gradlew -PgraphiteSurfacePath=../GraphiteSurface :KMaP:jvmTest
```

### Assets Web

Os samples do GraphiteSurface copiam manualmente:

- `skiko.mjs`;
- `skiko.wasm`;
- `skiko-graphite.mjs`;
- `skiko-graphite.wasm`;
- reexports necessários;
- `graphite-render-worker.mjs`.

O GraphiteSurface deve publicar uma configuração Gradle consumível ou uma task
que entregue esse conjunto. O `DemoApp:webApp` copia o artifact para a pasta de
distribution. O KMaP não deve conhecer caminhos internos do fork Skiko.

Gate Web: limpe os builds, gere a distribution e abra o resultado sem acessar
arquivos no checkout do GraphiteSurface.

### Release

O KMaP não pode publicar uma release que exige checkout irmão. Antes da release:

- publique `graphite-surface` e `graphite-engine`;
- publique os artifacts do fork Skiko com coordenada inequívoca;
- confirme variants Android, JVM, JS, Wasm e iOS no metadata KMP;
- teste uma aplicação consumidora vazia sem composite build;
- mantenha Compose como fallback quando o artifact da plataforma não existe.

## Hierarquia de source sets

O GraphiteSurface não oferece `macosArm64`. Não adicione a dependência a
`commonMain` enquanto isso continuar verdadeiro.

Crie um source set intermediário para os targets atendidos:

```kotlin
sourceSets {
    val commonMain by getting

    val graphiteMain by creating {
        dependsOn(commonMain)
        dependencies {
            implementation(libs.graphite.surface)
        }
        languageSettings.optIn(
            "com.rafambn.graphitesurface.ExperimentalGraphiteSurfaceApi"
        )
    }

    androidMain.get().dependsOn(graphiteMain)
    jvmMain.get().dependsOn(graphiteMain)
    jsMain.get().dependsOn(graphiteMain)
    wasmJsMain.get().dependsOn(graphiteMain)
    iosMain.get().dependsOn(graphiteMain)
}
```

Confirme os nomes reais da hierarquia. O resultado deve ser:

```text
commonMain
    |
    +-- graphiteMain
    |     +-- androidMain
    |     +-- jvmMain
    |     +-- jsMain
    |     +-- wasmJsMain
    |     +-- iosMain
    |           +-- iosArm64Main
    |           +-- iosSimulatorArm64Main
    |
    +-- macosArm64Main, apenas Compose
```

Se o compilador não aceitar o `actual` no source set intermediário, mantenha a
implementação comum em `graphiteMain` e crie actuals de uma linha nos leaf
source sets. Não duplique o controller ou a matemática.

## Arquivos propostos no KMaP

Use `com.rafambn.kmap.render`.

| Arquivo | Responsabilidade |
| --- | --- |
| `MapRenderBackend.kt` | enum público |
| `GraphiteCompatibility.kt` | análise pura do conteúdo e da style |
| `GraphiteMap.kt` | função `expect` que hospeda o renderer escolhido |
| `GraphiteMap.graphite.kt` | implementação compartilhada dos targets Graphite |
| `GraphiteMap.macos.kt` | actual indisponível do target sem dependência |
| `PlatformGraphiteSupport.*.kt` | disponibilidade real do host e runtime |
| `GraphiteMapController.kt` | engine, render callback e falha fatal |
| `GraphiteMapScene.kt` | snapshot imutável do frame lógico |
| `GraphiteMapCamera.kt` | valores da câmera usados na gravação |
| `GraphiteVectorCanvas.kt` | dados de um canvas vector |
| `GraphiteVectorBatch.kt` | um canvas e uma style layer, unidade de gravação |
| `GraphiteSceneCompiler.kt` | lê `KMaPContent` e `ActiveTiles` |
| `GraphiteVectorRenderer.kt` | grava os batches e apresenta o frame |

Cada tipo top-level fica no próprio arquivo. Remova qualquer arquivo da tabela
que termine como um wrapper de uma chamada. Não crie `Manager`, `Factory`,
`Repository` ou interface para uma única implementação.

## Mudança em `KMaP`

O DSL precisa ser avaliado tanto para escolher o backend quanto para formar o
`ComponentProvider`. Extraia o menor estado compartilhado possível:

```kotlin
@Composable
internal fun rememberKMaPContent(
    content: KMaPContent.() -> Unit,
    mapState: MapState,
): State<KMaPContent>
```

Mantenha `rememberComponentProviderLambda` como um adaptador desse estado se
isso reduzir a mudança no `LazyLayout`. Não avalie `KMaPContent` duas vezes,
pois seu `init` chama `canvasKernel.refreshCanvas`.

Estrutura esperada de `KMaP`:

```kotlin
val currentContent = rememberKMaPContent(content, mapState)
val incompatibility = currentContent.value.graphiteIncompatibility()
val useGraphite = resolveBackend(renderBackend, incompatibility, platformSupport)

if (useGraphite) {
    GraphiteMap(modifier, mapState, currentContent.value, onFatalError)
} else {
    ComposeMap(modifier, mapState, currentContent)
}
```

`ComposeMap` pode ser uma função privada no mesmo arquivo. Ela deve conter o
`LazyLayout` atual sem mudar provider, measure policy, placement, clipping ou
`onGloballyPositioned`.

O modifier que atualiza `mapState.setCanvasSize` precisa existir nos dois
branches. Evite disparar uma alteração de tamanho repetida quando o valor não
mudou.

## Modelo imutável do frame

O callback do renderer cruza coroutines e recorders. Ele deve receber um
snapshot de valores, não lambdas que leem estado Compose.

```kotlin
internal data class GraphiteMapCamera(
    val canvasSize: IntSize,
    val translation: Offset,
    val rotationDegrees: Float,
    val magnifierScale: Float,
    val positionOffset: Offset,
    val tileSizePx: Size,
    val zoom: Double,
)
```

Não transporte `CanvasDrawReference` mutável. Copie seus dois números para
`Offset`.

```kotlin
internal data class GraphiteVectorCanvas(
    val id: Int,
    val declarationIndex: Int,
    val zIndex: Float,
    val style: OptimizedStyle,
    val activeTiles: ActiveTiles,
)

internal data class GraphiteMapScene(
    val camera: GraphiteMapCamera,
    val canvases: List<GraphiteVectorCanvas>,
)
```

Copie a lista de tiles ao compilar a cena. `OptimizedVectorTile` e seus paths
podem ser compartilhados se forem imutáveis. Se alguma lista interna for
mutável, corrija a fronteira no pipeline de otimização. Não faça deep copy de
paths em cada frame.

Ordene canvases por `zIndex` e depois por `declarationIndex`. Essa segunda chave
preserva a ordem atual quando dois canvases têm o mesmo z.

## Compilação da cena

`GraphiteSceneCompiler` roda no lado Compose e executa apenas:

1. lê os números atuais de `MapState` usados pelo `VectorTileCanvas`;
2. lê `canvasKernel.getActiveTiles(parameters.id)` para cada vector canvas;
3. copia a lista de tiles;
4. ordena os canvases;
5. produz `GraphiteMapScene`.

Não faça tile lookup, fetch, decode ou otimização nesse compilador. Não crie um
segundo cache de tiles.

Use `snapshotFlow` ou um `SideEffect` simples de acordo com o estado existente.
O requisito é que mudanças de câmera, style ou `ActiveTiles` chamem
`renderer.invalidate()`. Não lance uma coroutine nova por propriedade.

## Ordem de desenho vector

O renderer Compose atual desenha, por canvas:

1. background nos tiles ativos;
2. style layers não-background na ordem declarada;
3. para cada style layer, todos os tiles ativos;
4. para cada tile, todas as features daquela layer.

O Graphite deve manter exatamente essa ordem. Não agrupe primeiro por tile,
pois isso muda a composição entre style layers.

Converta a cena em lotes ordenados:

```kotlin
internal data class GraphiteVectorBatch(
    val canvasId: Int,
    val canvasOrder: Int,
    val styleLayerIndex: Int,
    val styleLayer: OptimizedStyleLayer,
    val activeTiles: ActiveTiles,
)
```

Cada batch representa uma style layer completa de um canvas. A lista final é
ordenada por `canvasOrder` e `styleLayerIndex`. O frame insere recordings nessa
ordem, independentemente do recorder que as criou.

Background pode usar um batch com índice reservado antes das outras layers.
Não misture backgrounds de canvases distintos.

## Transform e matemática do tile

O transform externo deve repetir o `VectorTileCanvas` atual, na mesma ordem:

```text
translate(camera.translation)
rotate(camera.rotationDegrees, origin = 0,0)
scale(2 ^ camera.magnifierScale, origin = 0,0)
```

Para cada tile:

```text
scaleAdjustment = 2 ^ (activeTiles.currentZoom - tile.zoom)
sizeX = scaleAdjustment * tileSize.widthPx
sizeY = scaleAdjustment * tileSize.heightPx
positionX = floorLikeCurrentRenderer(positionOffset.x)
positionY = floorLikeCurrentRenderer(positionOffset.y)
tileLeft = tile.col * sizeX + positionX
tileTop = tile.row * sizeY + positionY

save
translate(tileLeft, tileTop)
scale(sizeX / extent, sizeY / extent)
clipRect(0, 0, extent, extent)
draw features
restore
```

Preserve o arredondamento atual, inclusive a diferença observada entre
background e features, até o teste visual provar que ele é um bug. Uma limpeza
de coordenadas é outro PR.

Use `GraphiteTransform` e `clipRect` diretamente. Não crie uma matriz paralela
do KMaP.

## Mapeamento de style

### Background

Para cada tile ativo:

- avalie `background-color` no zoom atual;
- avalie `background-opacity`;
- desenhe o retângulo do tile;
- desative antialias, como o renderer atual, e preserve o alpha avaliado.

### Fill

Para cada `OptimizedGeometry.Polygon`:

- avalie `fill-color` com propriedades da feature;
- avalie `fill-opacity`;
- desenhe todos os paths com fill;
- se existir `fill-outline-color`, desenhe stroke de largura `1f` depois do
  fill.

### Line

Para cada `OptimizedGeometry.LineString`:

- avalie `line-color`;
- avalie `line-width`;
- avalie `line-opacity`;
- avalie `line-cap` e `line-join`;
- use `width * scaleAdjustment`, como o renderer atual;
- desenhe o path com antialias.

### Symbol

Uma layer `symbol` visível torna a cena incompatível. Não omita labels e não as
substitua por retângulos. O próximo estágio deve adicionar texto ao
GraphiteSurface ou comprovar uma composição de overlays por plataforma.

### Expressões

Reutilize `OptimizedStyleLayer` e o evaluator atual. Avalie com o mesmo zoom,
properties e layer id do Compose. Não traduza o style para uma segunda AST.

Na primeira versão, avalie styles durante cada recording. Isso prioriza
correção para expressões dependentes de zoom e feature. Adicione cache somente
depois de medir e somente com uma chave que inclua todos os valores capazes de
mudar o resultado.

## Dois recorders

Use dois recorders estáveis no `GraphiteEngine`. A unidade de paralelismo é o
batch de style layer, não o canvas inteiro. Isso permite trabalho paralelo até
com um único `vectorCanvas`.

Atribuição sugerida:

```kotlin
val recorderIndex = (batch.canvasId * 31 + batch.styleLayerIndex)
    .mod(runtime.recorders.size)
```

Requisitos:

- a mesma chave mantém o recorder entre frames;
- batches do mesmo recorder preservam sua ordem local;
- recorders diferentes podem gravar em paralelo;
- `awaitAll` espera todas as recordings antes de criar o frame;
- o owner insere recordings pela ordem da lista de batches;
- recordings Kotlin não exigem `close` se a API atual já transfere ownership;
- o `GraphiteEngine` continua sendo o único owner do context e da apresentação.

Use `coroutineScope { async { ... } }`. Não use `GlobalScope`, mutex global ou
dispatcher como substituto para os workers nativos do engine.

Se um frame tiver somente um batch, use um recorder. Não invente uma divisão
de path para ocupar o segundo.

## Controller e invalidação

`GraphiteMapController` deve possuir apenas estado com lifecycle real:

- um `GraphiteEngine(recorderCount = 2)`;
- um `GraphiteRenderer` em `OnDemand`;
- a cena mais recente;
- a falha fatal, se ocorrer.

API interna suficiente:

```kotlin
internal class GraphiteMapController(
    onFatalError: (Throwable) -> Unit,
) : AutoCloseable {
    fun update(scene: GraphiteMapScene)
    override fun close()
}
```

`update` substitui a cena e invalida o renderer. O callback de frame lê um
snapshot consistente e grava os batches.

Não adicione `StateFlow`, repository, manager ou cache para uma cena atual. Se
o renderer exige acesso thread-safe, use o atomic simples já adotado pelo
GraphiteSurface.

O `DisposableEffect(controller)` fecha o controller. O scope que coleta estado
deve ser filho da composição e cancelado no mesmo efeito.

### Backpressure

Pan e zoom podem produzir estado mais rápido que a GPU. Use a semântica mais
recente do `GraphiteRenderer`:

- no máximo um frame em gravação/apresentação;
- uma cena pendente substituível;
- atualizações intermediárias podem ser descartadas;
- a cena mais recente precisa aparecer após o frame atual;
- nenhuma fila cresce com o número de eventos de pointer.

Não coloque cenas em `Channel.UNLIMITED`.

### Falhas

Capture erro onde existe uma decisão:

- erro de criação de runtime ou surface em `Auto`: feche o que abriu e selecione
  Compose;
- erro fatal de recording/present em `Auto`: feche o controller uma vez e
  selecione Compose;
- o mesmo erro em `Graphite`: entregue ao handler da composição e torne a falha
  visível;
- erro de tile continua seguindo o comportamento atual do `TileRenderer`.

Não envolva cada draw em `try/catch`. O engine e o controller são as fronteiras
de recurso.

## Tile pipeline

`KMaPContent.init` chama `canvasKernel.refreshCanvas`. Como o conteúdo é criado
antes da seleção do renderer, os mesmos `VectorCanvasEngine`s continuam vivos
em Compose e Graphite.

Preserve:

- `maxCacheTiles`;
- channels e coroutine scope atuais;
- normalização de row e column;
- seleção de parents e children;
- ordenação atual de `ActiveTiles`;
- `TileSource`, `TileResult`, `VectorTile` e `OptimizedVectorTile`.

Ao remover um canvas, `CanvasKernel.refreshCanvas` hoje remove a entrada do map,
mas confirme que o trabalho daquele engine também termina. Se o engine não
possui lifecycle próprio, corrija isso como uma mudança separada e testada. Não
atribua esse vazamento ao Graphite nem adicione `close` em valores Kotlin que
não possuem recurso nativo.

## Fases de implementação

### Fase 0: congelar baseline

1. registre os commits dos três repositórios;
2. execute os testes existentes;
3. capture screenshots Compose de um mapa só com fill/line;
4. capture screenshots de raster, vector com labels, markers, paths e clusters;
5. registre p50, p95, p99, CPU e memória do mapa vector de referência.

Gate: os resultados e os comandos ficam anexados ao PR ou a um arquivo de
benchmark versionado.

### Fase 1: provar a dependência

1. configure o composite build opcional;
2. adicione a dependência apenas ao source set atendido;
3. resolva classpaths e metadata;
4. compile todos os targets sem mudar `KMaP`;
5. abra o sample mínimo do GraphiteSurface nos targets candidatos.

Gate: KMaP compila com a dependência e o caminho Compose permanece idêntico.

### Fase 2: fechar stroke no GraphiteSurface

1. verifique suporte atual de width, cap, join e miter;
2. implemente somente os campos ausentes;
3. atualize protocolo e executores;
4. teste round trip e pixels;
5. publique ou disponibilize o artifact pelo composite.

Gate: todos os caps e joins usados pelo KMaP chegam ao Skia correto em cada
target. Nenhuma API de imagem entra nesta fase.

### Fase 3: adicionar a seleção sem ativar Graphite

1. adicione `MapRenderBackend`;
2. avalie `KMaPContent` uma vez;
3. implemente `graphiteIncompatibility`;
4. mantenha `Auto` resolvendo Compose em todas as plataformas;
5. teste raster e todos os motivos de incompatibilidade.

Gate: a suíte visual Compose não muda e `Graphite` forçado informa erros úteis.

### Fase 4: modelar a cena vector

1. crie os snapshots imutáveis;
2. leia `ActiveTiles` do kernel atual;
3. ordene canvases por z e declaração;
4. gere batches por style layer;
5. teste snapshots sem inicializar GPU.

Gate: um teste prova a ordem exata de canvas, layer, tile e feature.

### Fase 5: renderizar background, fill e line

1. implemente os transforms externos;
2. implemente matemática, clipping e fallback de tiles;
3. mapeie background;
4. mapeie fill e outline;
5. mapeie line, cap e join;
6. compare pixels com o Compose em câmera fixa.

Gate: screenshots passam na tolerância definida em Android e iOS antes de
qualquer ativação automática.

### Fase 6: ligar controller e recorders

1. crie um engine com dois recorders;
2. grave batches em paralelo;
3. insira recordings em ordem;
4. apresente em `OnDemand`;
5. conecte invalidação à cena mais recente;
6. feche o controller na saída da composição.

Gate: diagnostics volta a zero após sair da tela e nenhuma fila cresce no
stress de pan/zoom.

### Fase 7: integrar o branch em `KMaP`

1. implemente `GraphiteMap` nos source sets corretos;
2. preserve size e gestos do canvas;
3. conecte fallback fatal de `Auto`;
4. valide troca dinâmica entre conteúdos;
5. confirme que mapas raster nunca criam `GraphiteEngine`.

Gate: instrumentation prova zero inicializações Graphite em `SimpleMap` ou
qualquer tela raster.

### Fase 8: ativar por plataforma

Ative nesta ordem:

1. Android;
2. iOS Simulator;
3. iOS device;
4. Wasm browser;
5. JS browser.

Cada ativação exige build limpo, teste visual, input, resize, backgrounding e
stress. JVM permanece Compose até um trabalho próprio resolver o host e o gate
visual.

### Fase 9: release

1. publique artifacts reproduzíveis;
2. remova dependência do checkout irmão na validação final;
3. execute a matriz completa;
4. documente `MapRenderBackend` e as limitações;
5. preserve uma flag simples para voltar `Auto` a Compose.

Gate: uma aplicação consumidora limpa executa raster no Compose e o sample
vector compatível no Graphite.

## Testes unitários

### Seleção de backend

Cubra:

- nenhum canvas;
- somente raster;
- raster e vector juntos;
- vector com marker;
- vector com cluster;
- vector com path;
- vector com alpha parcial;
- vector com `symbol`;
- vector com type desconhecido;
- somente background, fill e line;
- plataforma suportada e não suportada;
- `Auto`, `Compose` e `Graphite`.

Teste que raster retorna o motivo antes de qualquer outra análise. A mensagem é
parte do teste do modo forçado.

### Ordenação

Cubra:

- canvases com z distintos;
- empate de z resolvido pela declaração;
- background antes das demais layers do mesmo canvas;
- style layers na ordem original;
- tiles na ordem do `ActiveTiles` atual;
- recordings concluídas fora de ordem e inseridas em ordem.

### Matemática

Cubra:

- tile no zoom atual;
- parent tile;
- child tile;
- row e column negativos quando permitidos;
- position offset positivo e negativo;
- rotação;
- magnifier scale fracionário;
- resize;
- clip no extent.

Use os mesmos números do renderer Compose e compare comandos ou coordenadas
esperadas, sem GPU.

### Styles

Cubra:

- background color e opacity;
- fill color, opacity e outline;
- line color, opacity, width, cap e join;
- expressão dependente de zoom;
- expressão dependente de propriedade da feature;
- fallback atual para propriedade ausente;
- symbol layer incompatível.

### Invalidação e lifecycle

Cubra:

- câmera nova invalida;
- tiles novos invalidam;
- style nova invalida;
- atualização idêntica não cria fila sem limite;
- cena mais nova substitui cena pendente;
- close cancela trabalho;
- close idempotente;
- falha fatal em `Auto` muda para Compose uma vez;
- raster não constrói controller.

## Testes de integração

Crie uma tela de diagnóstico vector-only sem symbol layers. Ela precisa mostrar:

- pelo menos dois fill layers sobrepostos;
- lines com butt, round e square;
- joins miter, round e bevel;
- dois níveis de zoom ativos para testar parent/child;
- dois vector canvases com zIndex diferentes;
- contador dos dois recorders nos diagnostics.

Também mantenha telas Compose de controle:

- um raster canvas;
- raster e vector juntos;
- vector com labels;
- vector com marker e path.

Nessas telas, prove que `GraphiteEngine` não foi criado.

### Interação

No sample Graphite, valide tap, double tap, long press, swipe, scroll, pinch e
rotação que já pertencem ao `MapGestureWrapper`. Compare a câmera final com o
renderer Compose após a mesma sequência.

### Stress

Durante cinco minutos:

- faça pan contínuo;
- alterne zoom entre dois níveis;
- gire continuamente;
- faça resize;
- entre e saia da tela;
- simule tile source lento e com falhas.

Ao final:

- memória estabiliza;
- filas voltam a zero;
- diagnostics não mostra recorder preso;
- não há validation errors da GPU;
- input continua responsivo;
- trocar para uma tela raster fecha o engine anterior.

## Validação por plataforma

### Android

- rode em API 24 e na API estável mais recente;
- teste Vulkan disponível e indisponível;
- teste background/foreground e rotação do device;
- confirme que o `SurfaceView` recebe tamanho físico correto;
- confirme que um mapa raster não cria nem anexa `SurfaceView`.

### iOS

- rode simulator e device;
- teste resize, safe area, background/foreground e navegação repetida;
- confirme escala de pixels da `CAMetalLayer`;
- confirme liberação de engine ao remover a view.

### Web

- teste Chrome e outro browser WebGPU aprovado;
- teste device WebGPU ausente em `Auto`;
- confirme carregamento dos Wasm e worker sem caminho local;
- teste resize e device loss;
- confirme que o worker de comandos atual preserva a ordem dos batches.

### JVM e macOS nativo

- compile e execute o fallback Compose;
- teste `Graphite` forçado e a mensagem de indisponibilidade;
- confirme que não existe referência a classe Graphite no artifact macOS nativo.

## Comandos de validação

Descubra os nomes atuais com `./gradlew tasks --all` se algum comando divergir.
Execute da raiz do KMaP:

```shell
rtk ./gradlew -PgraphiteSurfacePath=../GraphiteSurface \
  :KMaP:jvmTest \
  :KMaP:compileAndroidMain \
  :KMaP:compileKotlinJs \
  :KMaP:compileKotlinWasmJs \
  :KMaP:compileKotlinIosSimulatorArm64
```

Depois:

```shell
rtk ./gradlew -PgraphiteSurfacePath=../GraphiteSurface \
  :DemoApp:androidApp:assembleDebug \
  :DemoApp:webApp:wasmJsBrowserDistribution \
  :KMaP:linkReleaseFrameworkIosArm64
```

Finalize com as tasks `check` e `build` dos dois repositórios. Não marque uma
task ignorada por target como prova de execução.

## Comparação visual

Para cada cena de referência:

1. fixe viewport, zoom, rotação, density e tamanho em pixels;
2. espere todos os tiles finais carregarem;
3. capture Compose e Graphite;
4. compare o mesmo recorte pixel a pixel;
5. permita tolerância apenas nas bordas com antialias;
6. investigue diferenças sistemáticas de translate, scale, clip, alpha e width.

Parent e child tiles precisam de capturas próprias enquanto o tile final ainda
está pendente. Um screenshot feito somente depois do carregamento não testa o
fallback visual do `CanvasEngine`.

## Métricas

Compare Compose e Graphite na mesma cena vector-only e no mesmo device:

- CPU do main thread;
- tempo total de gravação;
- tempo por recorder;
- p50, p95 e p99 de frame;
- queue depth;
- frames aceitos, substituídos e rejeitados;
- command bytes por frame;
- memória após 1 e 5 minutos;
- quantidade de tile requests e tiles processados.

O Graphite pode aumentar trabalho total e ainda melhorar o main thread. Registre
os dois. Não declare ganho com base no FPS do simulator.

Critério recomendado para manter `Auto` ativo em um target:

- nenhuma regressão visual fora da tolerância;
- nenhuma regressão de input;
- p95 do main thread melhora ou permanece equivalente;
- memória estabiliza no stress;
- diagnostics fica sem backlog depois que a câmera para.

Se o mapa de referência tiver poucas style layers e usar só um recorder, isso é
um resultado válido. Não aumente a fragmentação apenas para produzir um gráfico
com dois workers ocupados.

## Próximas entregas

### Texto e symbols

O próximo passo útil é um comando Graphite de texto que preserve:

- font resolution multiplataforma;
- `text-field`, transform, size, max width e line height;
- anchor, offset, radial offset, translate e rotate;
- color, opacity, halo width, halo blur e halo color;
- orientação relativa à rotação do mapa.

Depois desse comando, remova o gate de `symbol` e acrescente testes de labels.
Ícones continuam separados porque o renderer Compose atual também não os
implementa.

### Paths do KMaP

Paths podem migrar quando Graphite tiver paridade de `DrawStyle`, `ColorFilter`,
`BlendMode`, `PathEffect`, alpha de layer e hit testing. Antes disso, um path faz
o mapa inteiro escolher Compose.

### Markers e clusters

Esses componentes aceitam composables arbitrários. Mantenha Compose como
renderer completo até existir uma estratégia de interop aprovada por target.
Não prometa uma única solução para SurfaceView, UIKit, Swing e DOM.

### Raster

Raster continua no Compose por decisão de arquitetura. Uma futura medição pode
reabrir esse tema, mas ela precisa justificar API de imagem, ownership de
bitmaps, uploads, cache GPU e composição com overlays. Nada disso é requisito
para a integração descrita aqui.

## Rollback

O rollback operacional é `renderBackend = MapRenderBackend.Compose` ou uma
alteração única em `PlatformGraphiteSupport` para `Auto` retornar Compose. O DSL,
tile pipeline e `MapState` continuam iguais.

Não mantenha dois pipelines públicos nem uma flag por feature. Um rollback
precisa trocar o renderer completo.

## Checklist final

### Dependência

- [ ] composite build funciona sem caminho absoluto;
- [ ] classpaths não misturam Skiko upstream e fork incompatíveis;
- [ ] artifacts publicados resolvem sem checkout irmão;
- [ ] assets Web vêm de uma configuração ou task consumível.

### Seleção

- [ ] raster sempre usa Compose;
- [ ] raster e vector juntos usam Compose;
- [ ] markers, clusters e paths usam Compose na primeira entrega;
- [ ] symbol layers usam Compose;
- [ ] alpha de canvas diferente de `1f` usa Compose;
- [ ] `Compose` nunca inicializa Graphite;
- [ ] `Graphite` informa a primeira incompatibilidade.

### Renderer vector

- [ ] ordem de canvas e style layer preservada;
- [ ] background, fill, outline e line têm paridade;
- [ ] cap e join chegam ao Skia;
- [ ] clipping por tile preservado;
- [ ] parent e child tiles preservados;
- [ ] transforms e arredondamentos cobertos por teste;
- [ ] recordings são inseridas em ordem visual;
- [ ] dois recorders recebem batches estáveis quando aplicável.

### Lifecycle

- [ ] engine fecha na saída da composição;
- [ ] scopes filhos são cancelados;
- [ ] cena pendente é substituível e limitada;
- [ ] device loss em `Auto` seleciona Compose uma vez;
- [ ] troca para conteúdo raster fecha Graphite;
- [ ] diagnostics volta a zero após a tela sair.

### Plataformas

- [ ] Android device;
- [ ] iOS simulator;
- [ ] iOS device;
- [ ] JS browser;
- [ ] Wasm browser;
- [ ] JVM fallback;
- [ ] macOS nativo fallback.

### Qualidade

- [ ] testes unitários passam;
- [ ] builds dos targets passam;
- [ ] screenshots estão dentro da tolerância;
- [ ] stress de cinco minutos estabiliza;
- [ ] métricas Compose e Graphite estão registradas;
- [ ] documentação pública explica `MapRenderBackend`.

## Definição de concluído

A migração termina quando um app consumidor limpo demonstra os dois caminhos:

1. um mapa raster real executa pelo renderer Compose atual sem criar qualquer
   objeto Graphite;
2. um mapa somente com vector canvases compatíveis desenha background, fill e
   line por uma única `GraphiteSurface`, usando batches ordenados e até dois
   recorders.

Os dois caminhos precisam preservar câmera, tiles ativos e gestos, passar a
matriz de builds e liberar seus recursos ao sair da composição. Suporte a
symbols, paths composables, markers, clusters ou raster no Graphite pertence a
entregas futuras e não bloqueia esta.
