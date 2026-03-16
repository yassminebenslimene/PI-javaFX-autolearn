import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SharedService {

  x = 1;

  constructor() { }   

  affich(){
    console.log(this.x);
  }

  todos =  [ {
    text: 'Learn Angular'
  }];

  
}
