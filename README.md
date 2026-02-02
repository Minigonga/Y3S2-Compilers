# Compiler Project

## Grade 18.72/20

## Participants
- Pedro Borges (up202207552)
- Manuel Mo (202205000)
- Gonçalo Pinto (202204943)

## Optimizations

### Ollir

- Register Allocation (Liveness, Graph Coloring and Interference Graph)
- Constant Propagation
- Constant Folding
- Removal of unnecessary assigns and var declarations on constant variables

### Jasmin

- Integer Constant Loading

    - iconst_m1 to iconst_5 for values -1 to 5

    - bipush for values -128 to 127

    - sipush for values -32768 to 32767

    - ldc for larger values

- Local Variable Access

    - iload_0 to iload_3 / aload_0 to aload_3 for slots 0-3

    - istore_0 to istore_3 / astore_0 to astore_3 for slots 0-3

    - Generic iload/aload for slots ≥4

- Array Operations

    - iaload for int[] element loading

    - iastore for int[] element storing

    - aaload/aastore for object arrays

- Comparison Operations

    - iflt, ifge, ifgt, ifle for comparisons with zero

    - if_icmplt, if_icmpge, etc. for general integer comparisons

    - Boolean results optimized to iconst_1/iconst_0

- Arithmetic Optimizations

    - iinc for local variable increments/decrements (-128 to +127)

    - iadd, isub, imul, idiv for integer arithmetic


## Contributions
- Gonçalo Pinto 33.33%
- Manuel Mo 33.33%
- Pedro Borges 33.33%
