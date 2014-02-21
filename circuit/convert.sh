for x in `ls *.bmp *.jpg`
do
convert $x ${x%.*}.png
rm $x
done
