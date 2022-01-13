#1/bin/bash
if hash thyrio_frontend 2>/dev/null; then
    echo  "Found thyrio_frontend"
    exit 0
else
    echo "thryio_frontend not found!"
    exit 1
fi