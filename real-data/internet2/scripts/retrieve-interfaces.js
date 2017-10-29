// run with 'nodejs retrieve-interfaces.js'

var http = require('http');
var cheerio = require('cheerio');
var interface_map = {};

var options = {
    host: 'vn.grnoc.iu.edu',
    path: '/Internet2/interfaces/interfaces-addresses.html',
    port: '80',
    //This is the only line that is new. `headers` is an object with the headers to request
    headers: {}
    
};

callback = function(response) {
    var str = ''
    response.on('data', function (chunk) {
	str += chunk;
    });

    response.on('end', function () {
	$ = cheerio.load(str);
	var table_header = $('tr.hlBG');
	var table = table_header.parent();
	$('tr', table).each(function(idx, tr1){
	    if(0 === idx) // ignore header
		return;
	    var router_name = null;
	    var data = {};
	    $(this).children().each(function(idx, td){
		switch(idx){
		case 0:
		    router_name = $(this).text();
		    // data['router'] = $(this).text();
		    break;
		case 1:
		    data['interface'] = $(this).text();
		    break;
		case 2:
		    data['interface'] = data['interface']+'.'+$(this).text();
		    break;
		case 3:
		    data['ipv4_addr'] = $(this).text();
		    break;
		case 4:
		    data['ipv6_addr'] = $(this).text();
		    break;
		case 5:
		    data['description'] = $(this).text();
		    break;
		}
	    });
	    if(router_name in interface_map){
		interface_map[router_name].push(data);
	    }else{
		interface_map[router_name] = [data];
	    }
	});
	for(var key in interface_map){
	    if(interface_map.hasOwnProperty(key)){
		console.log(key);
		var interfaces = interface_map[key];
		interfaces.forEach(function(val, idx, array){
		    var desc = 'idx' + idx + ':\t';
		    desc += (val['interface']+'\t');
		    desc += (val['ipv4_addr'] + '\t');
		    desc += (val['ipv6_addr'] + '\t');
		    desc += val['description'];
		    console.log(desc);
		});
	    }
	    console.log('\n');
	}
    });
}

var req = http.request(options, callback);
req.end();
