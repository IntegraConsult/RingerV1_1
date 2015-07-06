var index_array = ['all','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','Y','Z'];
var contact ={}
var contact_index ={};
var phoneState = "initialising";
var phoneVolume = 1; // 0 is of 1 is normal 2 is loud
                  
function EM_proxy(action,arguments) {
  var payload = {
    action: action,
    arguments: arguments
  }
  url = JSON.stringify(payload);
  //console.log(url);
  location.replace(url);
  
}

function show_state (state) {
  phoneState = state;
  //jQuery('.number').html(phoneState);
  jQuery('#phonestate-indicator').removeClass();
  jQuery('#phonestate-indicator').addClass(phoneState);
  var pad = jQuery('#pad-call');
  pad.removeClass();

  if (phoneState == "ready") {
    pad.addClass('digits third pad-action icon call');

  }
  else{
    pad.addClass('digits third pad-action icon hangup');

  }

  
  
}

function save_user (){
  var user = {
    name: jQuery('#page_5 #name').val(),
    code: jQuery('#page_5 #code').val(),
    phone: jQuery('#page_5 #phone').val(),
    
  };
  EM_proxy('saveUser',user);
}

function change_volume(delta) {
  phoneVolume += delta;
  if (phoneVolume > 2) phoneVolume = 2;
  if (phoneVolume <0 ) phoneVolume = 0;
  jQuery('#phonevolume-indicator').removeClass();
  jQuery('#phonevolume-indicator').addClass('volume_' + phoneVolume);
  
  EM_proxy ('phoneVolume',phoneVolume);
}
function switch_to_page (id) {
  jQuery('.page').hide();
  jQuery('#page_' + id).show(); 
}

function show_group (id) {
  if ( id == 0) {
    jQuery('li.separator').show();
    jQuery('li.contact').show();
  }
  else {
    //id is a number
    jQuery('li.separator').hide();
    jQuery('li.contact').hide();
    jQuery('li.group_' + index_array[id]).show();
  }  
}

function list_index(list_id,page_id) {
  lijst = '<div id="' + list_id + '">'; 
  lijst +=   '<ol>';
  for (i=0 ; i < index_array.length; i+=1 ){
    var group = index_array[i];
    lijst +=     '<li class="index group_' + group + '">'+ group + '</li>' ;
  }
  lijst  +=   '</ol>';
  lijst  +=   '<div class="clearfix"></div>';
  lijst  += '</div>';
  jQuery(page_id + " .index").html(lijst);
  
}

function list_contacts(json) {

  var contacts = JSON.parse(json);
  var group ='$';
  var lijst  = '<div id ="contact-list">';
  lijst +=   '<ol>';

  for (i=0 ; i < contacts.length ; i+=1) {
    contact_name = contacts[i].name;
    contact_name = contact_name.charAt(0).toUpperCase() + contact_name.slice(1);
    first_char   = contact_name.charAt(0);
    if ( first_char != group ){
      // render separator
      group  = first_char;
      lijst +=     '<li class="separator  group_' + group +'">' ;
      lijst +=       '<div class="details">';
      lijst +=         '<span class="contact-name">' + group + '</span>';
      lijst +=         '<span class="contact-phone hidden">0</span>';
      lijst +=       '</div>';
      lijst +=       '<div class="clearfix"></div>';
      lijst +=      '</li>';
    }
    //render contact
    lijst  +=     '<li class="contact group_' + group +'">' ;
    lijst  +=       '<div class="icon avatar">';
    lijst  +=       '</div>';
    lijst  +=       '<div class="details">';
    lijst  +=         '<span class="contact-name">' + contact_name +'</span>';
    lijst  +=         '<span class="contact-phone hidden">'+ contacts[i].phoneNumber + '</span>';
    lijst  +=       '</div>';
    lijst  +=       '<div class="clearfix"></div>';
    lijst  +=      '</li>';


  }
  lijst  +=   '</ol>';
  lijst  +=   '<div class="clearfix"></div>';
  lijst  += '</div>';

  jQuery('#page_3 .list').html(lijst);
  contact = jQuery("#contact-list ol li");
  contact.click(function(e){
    var target = jQuery(e.currentTarget);
    var phone_number = target.find('.contact-phone').html();
    call_number(phone_number);
  });   
}

function setWallet (wallet) {
	//console.log("setting wallet to " + wallet);
	jQuery('#wallet').html(wallet);
}

function list_calls (json) {
  var c = JSON.parse(json);
  var calls = c.parameters;
  var lijst  = '<div id ="calllist">';
  lijst +=   '<ol>';

  for (i=0 ; i < calls.length ; i+=1) {
    
    //render call
    lijst  +=     '<li class="call">' ;
    lijst  +=       '<div class="icon ringer">';
    lijst  +=       '</div>';
    lijst  +=       '<div class="details">';
    lijst  +=         '<span class="call-number inline">' + calls[i].number+'</span>';
    lijst  +=         '<span class="call-date inline">' + calls[i].loginDate +'</span>';
    lijst  +=         '<span class="call-time inline">' + calls[i].time +'</span>';
    lijst  +=       '</div>';
    lijst  +=       '<div class="clearfix"></div>';
    lijst  +=      '</li>';
  }
  lijst  +=   '</ol>';
  lijst  +=   '<div class="clearfix"></div>';
  lijst  += '</div>';

  jQuery('#calls-list').html(lijst);
  call = jQuery("#calllist ol li");
  call.click(function(e){
    var target = jQuery(e.currentTarget);
    var phone_number = target.find('.call-number').html();
    call_number(phone_number);
  });
}

function updateUI(wallet,jsonContacts) {
  setWallet(wallet);
  list_contacts(jsonContacts);

}

function add_key (number, key) {
  number.append(key);
  if (key == "+"){
	  EM_proxy('key',"plusSign");  
  }
  else {
	  EM_proxy('key',key);  
  } 
    
  
}
function call_number(phone_number) {
  // show calling page
  switch_to_page(4);
  // only initiate a call if there is a phone number to call to
  if (phone_number !='' ) {
   EM_proxy('call',phone_number);
  }
}

function hangup() {
  // show dialer page
  switch_to_page(0);
  // only initiate a call if there is a phone number to call to
  EM_proxy('hangup');
}

jQuery('document').ready(function (){
  list_index('contacts-index','#page_3');
  
  
  // debug
   //show_state("ready");
   
    switch_to_page(0);
    //list_calls('{"action":51,"url":"ringer\/service","parameters":[{"time":"16:12","number":" 3946706123012","loginDate":"2015-Jul-02"},{"time":"16:10","number":" 3940920","loginDate":"2015-Jul-02"},{"time":"16:10","number":" 39116","loginDate":"2015-Jul-02"},{"time":"16:9","number":" 39116","loginDate":"2015-Jul-02"},{"time":"16:9","number":" 39116","loginDate":"2015-Jul-02"},{"time":"16:7","number":" 39 349 124 5153","loginDate":"2015-Jul-02"},{"time":"16:7","number":" 39115","loginDate":"2015-Jul-02"},{"time":"16:5","number":" 39115","loginDate":"2015-Jul-02"},{"time":"16:5","number":" 39116","loginDate":"2015-Jul-02"},{"time":"15:49","number":" 39115","loginDate":"2015-Jul-02"}]}');
    //setWallet(10.1);
  
   //var json =            "[" +
                "{\"name\":\"Ad Langenkamp\",\"phoneNumber\":\"00393336948230\"}," +
                "{\"name\":\"Els Vink\",\"phoneNumber\":\"00393272281874\"}" +
                "]";
  //list_contacts(json);
 
  // end debug
  

  var dials = jQuery(".dials ol li");
  var zeroKey= jQuery(".digits.second.icon.zero");
  var zeroKeyTimer;
  var zeroKeylongPress = false;
  var selection = jQuery(".header.prime ol li");
  var subSelection = jQuery(".header.sub ol li");
  var contacts_index = jQuery("#contacts-index ol li");
  var favourites_index = jQuery("#favourites-index ol li");
    
  
  var page_index = 0;
  var sub_page_index = 0;
  var index = 0 ;
  
  
  var number = jQuery(".number");
  
  var total;
  
  var pad_action = jQuery('.pad-action');
  
  
  
   
    
  
  
  contacts_index.click(function(){
    id = contacts_index.index(this);
    show_group(id);
  });
  
  
  
 
  selection.click(function(){
    page_index = selection.index(this);
    selection.removeClass('selected');
    subSelection.removeClass('selected');
    
    var choice = jQuery(".header ol li").eq(page_index);
    choice.addClass('selected');
    jQuery('.page').hide();
    jQuery('#page_' + page_index).show();
    
  });
  
  
  subSelection.click(function(){
	    sub_page_index = subSelection.index(this);
	    subSelection.removeClass('selected');
	    var choice = jQuery(".header.sub ol li").eq(sub_page_index);
	    choice.addClass('selected');
	    jQuery('.page.sub').hide();
	    jQuery('#page_' + page_index + '_' + sub_page_index).show();
	    if ((page_index==1)&&(sub_page_index==0)) {
	      // this is the calls log page
	      EM_proxy("getCalls",null);
	    }
	    
	  });
  
  // dialpad event handlers 
  zeroKey.mousedown(function(){
	  // start keyTiomer
	  zeroKeyTimer= setTimeout(function(){
		  add_key(number, "+");
		  zeroKeylongPress= true;
	  },500);
	  
  });
  zeroKey.mouseup(function(){
	  // stop the timer
	  clearTimeout(zeroKeyTimer);
	  if (!zeroKeylongPress) {
		  add_key(number, "0");  
	  }
	  zeroKeylongPress=false;
  });
  
  dials.click(function(){
    //audio_keyclick.play();
    index = dials.index(this);
    if(index == 9){
      if (phoneState == 'ready') add_key(number, "*");
      else change_volume(-1);
    }else if(index == 10){
       //add_key(number, "0");
       // do nothing because this key is handled by the zeroKey mousedown and zerKey.mouseup handlers
       }else if(index == 11){
           if (phoneState == 'ready') add_key(number, "#");
           else change_volume(+1);
        
         }else if(index == 12){
           number.empty();
           }else if(index == 13){
             total = number.text();
             total = total.slice(0,-1);
             
             number.empty().append(total);
             
             }else if(index == 14){
               if (phoneState== 'ready'){
                  // only initiate a call if there is a phone number to call to
                  var phone_number = jQuery('.number').html();
                  if (phone_number !='' ) {
                	  // make the call
                	  // switch_to_page(4);
                	  // replace + with urlsafe "plusSign"
                	  phone_number = phone_number.replace(/\+/g, 'plusSign');
                      EM_proxy('call',phone_number);
                  }

               }
               else {
                 EM_proxy('hangup');

               }

             }else{ 
               add_key(number, index+1);
             }
    
    
  });
     
  
 });
