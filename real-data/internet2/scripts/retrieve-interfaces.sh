#!/bin/bash
switch_ips=("64.57.28.253" "64.57.20.210" "64.57.28.254" "64.57.20.199" "64.57.28.251" "64.57.28.252" "64.57.28.243" "64.57.28.241" "64.57.24.157" "64.57.28.244" "64.57.28.245" "64.57.28.248" "64.57.28.242" "64.57.28.246" "64.57.28.247" "64.57.28.249")
switch_names=("ASHB" "DALL" "EQCH" "EQNY" "PAIX" "WILC" "ATLA" "CHIC" "CLEV" "HOUS" "KANS" "LOSA" "NEWY32AOA" "SALT" "SEAT" "WASH")
switch_len=16

DDIR=../data/$(date +%Y-%m-%d)
if [ "$1" != "" ]; then
    DDIR=$1/$(date +%Y-%m-%d)
fi
echo Output interfaces to "$DDIR"

base_url="http://routerproxy.grnoc.iu.edu/internet2/index.cgi?fname=getResponse&args=show%20interface&cmd=show%20interface&args=&args="
referer="Referer: http://routerproxy.grnoc.iu.edu/internet2/"
agent="User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36"

if [ ! -d $DDIR ]; then
    mkdir -p $DDIR
fi

for idx in $(seq 0 $((switch_len-1))); do
    device=${switch_ips[$idx]}
    url="${base_url}&args=${device}&device=${device}"
    output="${DDIR}/${switch_names[$idx]}.intf"
    wget "$url" --referer "$referer" -U "$agent" -O "$output"

    sed -i -e "s/&nbsp;/ /g" -e "s///g" "${output}"

    content_beg_line=$(grep -n "Physical interface" "${output}" | cut -d ":" -f1 | head -1)
    # remove all lines before content_beg_line
    sed -i -e "1,$((content_beg_line-1))d" "${output}"
    # remove all characters before "Physical interface..."
    sed -i -e 's/.*\(Physical interface.*\)/\1/1' "${output}"
    
    # remove all lines after </pre>
    sed -i -e "/<\/pre>/,\$d" "${output}"
done

