# ONGRID Extensões

Repositório pessoal de extensões compatíveis com Mihon.

## Fontes incluídas

- Lycan Toons (`pt-BR`)
- Ninja Comics (`pt-BR`)
- Taiyo (`pt-BR`)

O código-fonte fica na branch `main`. O GitHub Actions compila, assina e publica
automaticamente os APKs e o `index.min.json` na branch gerada `repo`.

## Como adicionar uma nova fonte

1. Gere o módulo pelo Mihon Source Studio (analisa o site e baixa um `.zip`).
2. Copie a pasta `src/pt/<pacote>` do zip para `sources/<pacote>/` neste repositório
   (removendo o prefixo `src/pt/`).
3. Adicione os ícones em `sources/<pacote>/res/mipmap-*/ic_launcher.png`
   (5 tamanhos: 48/72/96/144/192px).
4. Se o site não for Madara, complete os métodos `TODO` do Kotlin gerado.
5. Dê `git push` na branch `main`. O workflow descobre a pasta automaticamente,
   compila, assina e publica — sem precisar editar nenhum arquivo de índice manual.

## URL do repositório no Mihon

Depois que o primeiro workflow terminar:

```text
https://raw.githubusercontent.com/gabrieldesignhd-rgb/ONGRID-EXTENSOES/repo/index.min.json
```

## Segredos necessários no GitHub

- `SIGNING_KEY`: arquivo JKS codificado em Base64.
- `KEY_ALIAS`: alias da chave.
- `KEY_STORE_PASSWORD`: senha do arquivo JKS.
- `KEY_PASSWORD`: senha da chave.

Nunca publique o arquivo JKS ou suas senhas. Guarde uma cópia segura: todas as
atualizações futuras precisam ser assinadas com a mesma chave.

## Base técnica

O catálogo, os APKs, a assinatura e a URL publicada pertencem a este
repositório. A compilação usa bibliotecas abertas compatíveis com Mihon e o
adaptador Lycan Toons sob a licença Apache 2.0, preservada em `LICENSE`.
