# PICORV32


To make this compile I had to implement the following in Thyrio:
```
CELL_OR -> when width are not aligned
CELL_GE -> from scratch
CELL_MEMWR -> in ST had to select the correct width for base + ptr like LD
```