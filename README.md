# Gabriel Mihon Extensions

Repositório pessoal de extensões compatíveis com Mihon.

## Fonte incluída

- Lycan Toons (`pt-BR`)

O código-fonte fica na branch `main`. O GitHub Actions compila, assina e publica
automaticamente o APK e o `index.min.json` na branch gerada `repo`.

## URL do repositório no Mihon

Depois que o primeiro workflow terminar:

```text
https://raw.githubusercontent.com/gabrieldesignhd-rgb/mihon-extensions/repo/index.min.json
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
