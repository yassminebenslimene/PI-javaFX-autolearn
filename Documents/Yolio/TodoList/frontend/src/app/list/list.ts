import { Component } from '@angular/core';
import { SharedService } from '../service/shared-service';

@Component({
  selector: 'app-list',
  imports: [],
  templateUrl: './list.html',
  styleUrl: './list.css',
})
export class List {

  constructor( public sharedService: SharedService) { }

  

}
