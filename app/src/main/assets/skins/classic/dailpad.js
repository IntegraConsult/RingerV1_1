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
    var index = ['all','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','Y','Z'];
    jQuery('li.separator').hide();
    jQuery('li.contact').hide();
    jQuery('li.group_' + index[id]).show();
  }  
}

jQuery('document').ready(function (){

  var dials = jQuery(".dials ol li");
  var contact = jQuery("#contact-list ol li");
  var contacts_search = jQuery("#contact-list .search-box");
  
  
  /*
  var contacts = jQuery("#contact-list ol li.contact .contact-name");
  var contact_names = [];
  for (i=0 ; i < contacts.length; i+=1) {
    var span = contacts[i];
    var name = span.innerText;
    contact_names.push(name);
  }
  
  contacts_search.autocomplete({
      source: contact_names
  });
  */
  
  var selection = jQuery(".header ol li");
  var contacts_index = jQuery("#contacts-index ol li");
  var favourites_index = jQuery("#favourites-index ol li");
  
  var page_index = 0;
  var index = 0 ;
  
  
  var number = jQuery(".number");
  var total;
  
  var pad_action = jQuery('.pad-action');
  var picked_up = false;
  var audio_keyclick = jQuery('.audio-keyclick')[0];
  var audio_volume = jQuery('.audio-volume');
  var volume = audio_volume.data('volume');
  audio_keyclick.volume = volume;
  
  
  
   
  switch_to_page(0);
    
  contact.click(function(e){
    var target = jQuery(e.currentTarget);
    var phone_number = target.find('.contact-phone').html();
    call_number(phone_number);
  });
  
  contacts_index.click(function(){
    id = contacts_index.index(this);
    show_group(id);
  });
  
  favourites_index.click(function(){
    id = favourites_index.index(this);
    show_group(id);
  });
  
 
  selection.click(function(){
    page_index = selection.index(this);
    selection.removeClass('selected');
    var choice = jQuery(".header ol li").eq(page_index);
    choice.addClass('selected');
    jQuery('.page').hide();
    jQuery('#page_' + page_index).show();
    
  });
  
  dials.click(function(){
    audio_keyclick.play();
    index = dials.index(this);
    if(index == 9){
      number.append("*");
    }else if(index == 10){
       number.append("0");
       }else if(index == 11){
         number.append("#");
         }else if(index == 12){
           number.empty();
           }else if(index == 13){
             total = number.text();
             total = total.slice(0,-1);
             number.empty().append(total);
             }else if(index == 14){
               //add any call action here
               if (picked_up = !picked_up) {
                 // make the call
                 call();
                 pad_action.removeClass('icon-call');
                 pad_action.addClass('icon-hangup');
               }
               else {
                 // hang up the phone
                 hangup();
                 pad_action.removeClass('icon-hangup');
                 pad_action.addClass('icon-call');
                 
               }
             }else{ 
                 number.append(index+1); 
             }
    
    
  });
     
  
 });
