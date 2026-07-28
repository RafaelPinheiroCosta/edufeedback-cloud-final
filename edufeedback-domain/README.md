# edufeedback-domain

Núcleo com modelos, enums e regras de negócio independentes de Azure, HTTP e banco de dados.

## Responsabilidades

- representar avaliações e níveis de urgência;
- definir eventos e contratos usados pelos demais módulos;
- concentrar regras que não dependem de frameworks de entrada ou infraestrutura.

## Build

```bash
mvn -pl edufeedback-domain -am clean verify
```
