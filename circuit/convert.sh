for x in `ls *.bmp`
do
convert $x ${x%.*}.png
rm $x
done
