var settings = {
  notifications: {
    caught_pokemon: true,
    next_level: true
  },
  autofollow: true,
  monitoringlog: true
};
var elements;
var icons = {
  trainer: 'images/trainer.png',
  pokemon: function(id){ return 'http://pokeapi.co/media/sprites/pokemon/' + id + '.png'; },
  pokestop: 'images/pokestop.png',
  goto:'images/goto.png'
};

var token;
var circle;
var circleInit = false;
var polyLine;
var map;
var positionMarker;
var pokestopMarkers = {};
var pokebank = {};
var caughtPokemonMarkers = [];
var gotoMarkers = [];
var socket;
var logCount = 0;
var selectedPokemon;

function goto(lat, lng){
  socket.emit('goto', {
    lat: lat,
    lng: lng
  });
  var marker = new google.maps.Marker({
    position: {
      lat: lat,
      lng: lng
    },
    icon: {
      url: icons.goto,
      scaledSize: new google.maps.Size(35, 35)
    },
    map: map
  });
  gotoMarkers.push(marker);
}

function init() {
    map = new google.maps.Map(document.getElementById('map'), {
      center: {lat: 0, lng: 0},
      zoom: 16
    });
    google.maps.event.addListener(map, "rightclick", function(event) {
      var lat = event.latLng.lat();
      var lng = event.latLng.lng();
      goto(lat, lng);
    });
    polyLine = new google.maps.Polyline({
       path: [],
      geodesic: true,
      strokeColor: '#FF0000',
      strokeOpacity: 1.0,
      strokeWeight: 2,
      map: map
    });
    positionMarker = new google.maps.Marker({
      position: {
        lat: 0,
        lng: 0
      },
      icon: {
        url: icons.trainer,
        scaledSize: new google.maps.Size(48, 48)
      },
      map: map
    });
}
function sendNotification(title, options, duration){
  if(Notification.permission === 'granted'){
    var notification = new Notification(title, options);
    if(typeof duration !== 'undefined'){
      setTimeout(function(){
        notification.close();
      }, duration);
    }
  } else {
    Notification.requestPermission(function(){
      if(Notification.permission === 'granted'){
        sendNotification(title, options)
        }
    });
  }
}
function runSocket(){
  init();
  socket.on('profile', function(data){
    if(typeof data.username !== 'undefined'){
      elements.profile.name.text(data.username);
      elements.profile.name2.text(data.username);
      document.title = data.username+ " : Poppo - PokemonGoBot GUI"
    }
    if(typeof data.team !== 'undefined'){
      data.team && elements.profile.team.text(data.team);
      data.team && elements.profile.team_badge.addClass(data.team+"-badge");
      if(!circleInit){
        circleInit = true;
        var TeamColor = (data.team=="BLUE")?"#2196f3":(data.team=="RED")?"#f44336":(data.team=="YELLOW")?"#fdd835":"#9e9e9e";
        $("#username-1").css("color",TeamColor);
        circle = new ProgressBar.Circle('#lvl', {
              color: TeamColor,
              strokeWidth: 2.1,
              duration: 1000,
              text:{
                style:{
                  "font-size":"12px",
                  "margin-top": "-20px",
                  "margin-left": "5px",
                },
              },
          });
      }
    }
    if(typeof data.stardust !== 'undefined'){
      elements.profile.stardust.text(data.stardust);
    }
    if(typeof data.levelRatio !== 'undefined'){
      circle.animate(data.levelRatio/100);
    }
    if(typeof data.level !== 'undefined' && typeof data.levelXp !== 'undefined'){
      if(typeof currentLevel !== 'undefined' && data.level > currentLevel && settings.notifications.next_level){
          sendNotification('You are now on level ' + data.level + '!', {
            icon: icons.trainer,
            lang: 'en'
            }, 5000);
        }
        currentLevel = data.level;
        circle.setText(data.level);
        elements.profile.level.text(data.level + ' (' + data.levelXp + ' XP)');
    }
    if(typeof data.pokebank !== 'undefined' && typeof data.pokebankMax !== 'undefined'){
      elements.profile.pokebank.text(data.pokebank + ' / ' + data.pokebankMax);
    }
    if(typeof data.items !== 'undefined' && typeof data.itemsMax !== 'undefined'){
      elements.profile.items.text(data.items + ' / ' + data.itemsMax);
    }
  });
  socket.on('pokebank', function(data){
    data.pokemon.sort(function(a, b){
      return b.cp - a.cp;
    });
    for(var i = 0; i < data.pokemon.length; i++){
      var pokemon = data.pokemon[i];
      var id = String(pokemon.id);
      pokebank[id] = {
         pokemonId: pokemon.pokemonId,
         name: pokemon.name,
         cp: pokemon.cp,
         iv: pokemon.iv,
         stats: pokemon.stats
      };
      var elem = $('<tr id="pokemon-id-'+id+'"><td><img src="' + icons.pokemon(pokemon.pokemonId) + '"></td><td>' + pokemon.name + '</td><td>' + pokemon.cp + '</td><td>' + pokemon.iv + '</td></tr>');
      elements.pokemonList.append(elem);
    }
  });
  socket.on('newPokemon', function(data){

    if(settings.notifications.caught_pokemon){
      sendNotification("Caught '" + data.name + "' with CP " + data.cp + "", {
        icon: icons.pokemon(data.pokemonId),
        lang: 'en'
      }, 1500);
    }
    var marker = new google.maps.Marker({
      position: {
        lat: data.lat,
        lng: data.lng
      },
      icon: {
        url: icons.pokemon(data.pokemonId),
        scaledSize: new google.maps.Size(70 , 70)
      },
      map: map,
      title: data.name + ' with CP ' + data.cp
    });
    caughtPokemonMarkers.push(marker);
  getAllPokemon();
	console.log("Pokemon added");
  });
  socket.on('releasePokemon', function(data){
    getAllPokemon();
    var id = String(data.id);
    if(typeof pokebank[id] !== 'undefined'){
      pokebank[id] = undefined;
      delete pokebank[id];
      $('#pokemon-id-' + id).remove();
    }
    	console.log("Pokemon removed");
  });
  socket.on('newLocation', function(data){
    if(typeof positionMarker !== 'undefined'){
      positionMarker.setPosition( new google.maps.LatLng( data.lat, data.lng ) );
    }
    if(typeof polyLine !== 'undefined'){
      var path = polyLine.getPath();
      path.push(new google.maps.LatLng(data.lat, data.lng));
      polyLine.setPath(path)
    }
    if(settings.autofollow){
      map.panTo(new google.maps.LatLng(data.lat, data.lng));
    }
  });
  socket.on('pokestop', function(data){
    var id = data.id;
    if(typeof data[id] === 'undefined' && typeof map !== 'undefined'){
     pokestopMarkers[id] = new google.maps.Marker({
       position: {
         lat: data.lat,
         lng: data.lng
       },
       map: map,
       icon: {
         url: icons.pokestop,
         scaledSize: new google.maps.Size(40 , 40)
       },
       title: data.name
     });
    }
  });
  socket.on('eggs', function(data){
  });
  socket.on('log', function(data){
    if(settings.monitoringlog){
      var span = $('<span class="' + data.type + '">' + data.text + '</span><br>');
      logCount++;
      elements.log.append(span);
      if(logCount> 100){
        elements.log.find('span:first').remove();
        elements.log.find('br:first').remove();
        logCount = 100;
      }
      elements.logParent.scrollTop(elements.logParent.prop("scrollHeight"));
    }
  });
  socket.on('gotoDone', function(data){
    gotoMarkers[0].setMap(null);
    gotoMarkers.shift();
  });
  socket.emit('init');
}
$(function() {
  $('.dropdown-button').dropdown({
      inDuration: 300,
      outDuration: 225,
      constrain_width: false, // Does not change width of dropdown to that of the activator
      hover: true, // Activate on hover
      gutter: 0, // Spacing from edge
      belowOrigin: false, // Displays dropdown below the button
      alignment: 'right' // Displays dropdown with edge aligned to the left of button
    }
  );
  $('.modal-trigger').leanModal({
     dismissible: true, // Modal can be dismissed by clicking outside of the modal
     opacity: .5, // Opacity of modal background
     in_duration: 300, // Transition in duration
     out_duration: 200, // Transition out duration
     starting_top: '4%', // Starting top style attribute
     ending_top: '10%', // Ending top style attribute
     ready: function() {  }, // Callback for Modal open
     complete: function() {} // Callback for Modal close
   }
  );
  elements = {
    body: $('body'),
    profile: {
      name: $('#username-1'),
      name2: $('#username-2'),
      team: $('#profile_team'),
      team_badge: $("#profile_team_badge"),
      stardust: $('#profile_stardust'),
      level: $('#profile_level'),
      level_progress: $('#profile_level_progress'),
      pokebank: $('#profile_pokebank'),
      pokebank_progress: $('#profile_pokebank_progress'),
      items: $('#profile_items'),
      items_progress: $('#profile_items_progress')
    },
    pokemonList: $('#pokemon_list'),
    log: $('#log'),
    logParent: $('#logParent')
  };
  $("#setting-map-autofollow").change(function(){
    settings.autofollow = !settings.autofollow;
  });
  $("#setting-noti-lvlup").change(function(){
    settings.notifications.next_level = !settings.notifications.next_level;
  });
  $("#setting-noti-caught").change(function(){
    settings.notifications.caught_pokemon = !settings.notifications.caught_pokemon;
  });
  $("#setting-log").change(function(){
    settings.monitoringlog = !settings.monitoringlog;
    if(settings.monitoringlog){
      $("#logDisplayTrigger").show();
    }else{
      $("#logDisplayTrigger").hide();
      elements.log.empty();
    }
  });
  if(("Notification" in window) && Notification.permission !== 'granted') {
    Notification.requestPermission(function(){});
  }
  $("#login").click(function(){
    getToken(); //START FUNCTION FOR GET REST API FUNCTIONS
    $("#login").hide();
    $("#login-preload").show();
    var address = $("#server-address").val();
    var port = $("#server-port").val();
    window.socket = io.connect('ws://' + address + ':' + port);
    var connectionTimeout = setTimeout(function() {
      sendNotification("Connection Timeout!",{
        icon: icons.trainer,
        lang: 'en'
        }, 5000);
      $("#login").show();
      $("#login-preload").hide();
    }, 5000);
    socket.on('connect', function() {
      window.clearTimeout(connectionTimeout);
      runSocket();
      $("#LoginScreen").hide();
      $("#map").show();
      $("#top-badge").show();
    })
  });
});


/**
New methods added
*/
function pokebankUpdate() {
  getAllPokemon();
  drawPokemon();
}

function drawPokemon() {
	var table = "";



	for (i = 0; i < allPokemon.length; i ++) {
    var favoriteicon = "";
		pok = allPokemon[i];
    if (pok.favorite==true){
      favoriteicon = "<img style ='height:50px;'src='http://orig15.deviantart.net/9102/f/2011/046/e/a/sexy_girl_icon_02_by_thyrring-d39mxjk.png'>";
    } else {
      favoriteicon = "";
    }
		  var icon = 'http://pokeapi.co/media/sprites/pokemon/' + pok.pokemonId + '.png';
		//Build GUI
		table = table + "<tr><td><img style ='height:50px;'src='" + icon + "'></td><td><b>" + pok.name + "</b></td><td>" + " " + pok.nickname + " </td><td> "+pok.cp+"" + "</td><td>"+pok.iv+"</td><td>" + favoriteicon + "</td><td> <button onclick='editPokemonModal("+pok.id+")' class='btn btn-default' >More options</button></td></tr>";
	}
	$("#pokemonList").html(""); //Clear the table so we can redraw with no problems and old gay pokemon
	$("#pokemonList").html(table);
	$(".border").stupidtable();
}



function getToken () {
    var restport = $("#REST-PORT").val();
    var restuser = $("#REST-API-User").val();
    var address = $("#server-address").val();
    var RESTURL = ('http://' + address + ':' + restport + '/api/bot/'+ restuser);



    var data = $("#REST-API-PW").val();

    var xhr = new XMLHttpRequest();
    xhr.withCredentials = true;

    xhr.addEventListener("readystatechange", function () {
		if (this.readyState === 4) {
			token = this.responseText;
			console.log(token); //Print token for dev purposes
			getAllPokemon();
		}
    });

  xhr.open("POST", RESTURL + '/auth');
  xhr.send(data);
}


function getAllPokemon() {

	var url = getUrl()+"pokemons";
	console.log(url);
	$.ajax({
		url: url,
		type: 'GET',
		timeout: 9000,
		headers: {
			"X-PGB-ACCESS-TOKEN" : token
		},
		cache: false,

		success: function(response) {
			allPokemon = response;

			//Because I'm lazy and want to get sushi fuck you
			function SortByName(a, b){
				var aName = a.iv;
				var bName = b.iv;
				return ((aName > bName) ? -1 : ((aName < bName) ? 1 : 0)); //Should I really rename this? We need an intern!
			}
			allPokemon.sort(SortByName);
      console.log(response);
			drawPokemon();
			},
		error: function(e) {
			console.log("Failed to get all pokemon");

		}

	});
}

function editPokemonModal(id){
	var pokemon = getPokemonById(id);
	selectedPokemon = pokemon;


	$("#pokemonName").text(pokemon.name);
	$("#editIcon").attr("src","https://pokeapi.co/media/sprites/pokemon/" + pokemon.pokemonId + ".png");
	console.log(pokemon);
	if (pokemon.candy >= pokemon.candiesToEvolve && pokemon.candiesToEvolve != 0)  {
		evolveButton = $('#evolveButton').prop("disabled",false);
	}  else {
		evolveButton = $('#evolveButton').prop("disabled",true);
	}
	$(".star.glyphicon").click(function() {
  $(this).toggleClass("glyphicon-star glyphicon-star-empty");
});
	$("#pokCP").text(pokemon.cp);
	$("#pokIV").text(pokemon.iv);
	$("#nicknameInput").text(pokemon.nickname);

	$(".pokemonListDiv").hide();
	$(".editPokemon").show();
	$("#close").hide();
	$("#return").show();
	console.log(id);
}

function returnToList() {
	getAllPokemon();
	$('#nicknameInput').val('');
	$('#nicknameInput').attr('placeholder','Enter Nickname');
  $('#nicknameButton').text("Change Nickname");

	selectedPokemon = null;

	$(".pokemonListDiv").show();
	$(".editPokemon").hide();
	$("#close").show();
	$("#return").hide();
}



function getUrl() {
  var restaddress = $("#server-address").val();
  var restport = $("#REST-PORT").val();
  var restname = $("#REST-API-User").val();

  return "http://" + restaddress + ":" + restport + "/api/bot/" + restname + "/";
}

function getPokemonById(id) {
	for(var i = 0; i < allPokemon.length; i++){
		if (allPokemon[i].id == id) {
			return allPokemon[i];
		}
	}
}


/**
Pokemon manipulation methods..
*/
function changeNickname() {
	$('#nicknameButton').text("Hold on, busy");

	nicknamePokemon(selectedPokemon.id,$('#nicknameInput').val());
}


function nicknamePokemon(pokeID,nickname) {
var url = getUrl()+"pokemon/"+pokeID+"/rename";
	$.ajax({
		url: url,
		type: 'POST',
		timeout: 9000,
		data: encodeURIComponent($('#nicknameInput').val()),
		contentType: "text/xml",
		dataType: "text",
		headers: {
			"X-PGB-ACCESS-TOKEN" : token
		},
		cache: false,
		success: function(response) {
			$('#nicknameButton').text("Nickname changed!");
		},
		error: function(e) {
			$('#nicknameButton').text("Something went wrong");

		}

	});
}

function evolvePokemon() {
var url = getUrl()+"pokemon/"+selectedPokemon.id+"/evolve";
	$.ajax({
		url: url,
		type: 'POST',
		timeout: 9000,
		headers: {
			"X-PGB-ACCESS-TOKEN" : token
		},
		cache: false,
		success: function(response) {
			console.log("geovuleerd");
			returnToList();
		},
		error: function(e) {
			console.log("SERVER DOWN!!!");

		}

	});
}

function transferPokemon() {
var url = getUrl()+"pokemon/"+selectedPokemon.id+"/transfer";
	$.ajax({
		url: url,
		type: 'POST',
		timeout: 9000,
		headers: {
			"X-PGB-ACCESS-TOKEN" : token
		},
		cache: false,
		success: function(response) {
			console.log("transfered");
			returnToList();
		},
		error: function(e) {
			console.log("SERVER DOWN!!!");

		}

	});
}

function favoritePokemon() {
var url = getUrl()+"pokemon/"+selectedPokemon.id+"/favorite";
  $.ajax({
    url: url,
    type: 'POST',
    timeout: 9000,
    headers: {
      "X-PGB-ACCESS-TOKEN" : token
    },
    cache: false,
    success: function(response) {
      console.log("favorited");
      returnToList();
    },
    error: function(e) {
      console.log("SERVER DOWN!!!");

    }

  });
}

function powerPokemon() {
var url = getUrl()+"pokemon/"+selectedPokemon.id+"/powerup";
  $.ajax({
    url: url,
    type: 'POST',
    timeout: 9000,
    headers: {
      "X-PGB-ACCESS-TOKEN" : token
    },
    cache: false,
    success: function(response) {
      console.log("powerupped");
      returnToList();
    },
    error: function(e) {
      console.log(e);

    }

  });
}
