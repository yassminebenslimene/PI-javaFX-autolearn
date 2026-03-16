import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SharedService } from '../service/shared-service';

@Component({
  selector: 'app-create',
  imports: [FormsModule],
  templateUrl: './create.html',
  styleUrl: './create.css',
})
export class Create {

  constructor( private sharedService: SharedService) { }

  todo = { text: ''};
  

  addtodo() {
    this.sharedService.todos.push(this.todo);
    this.todo = { text: '' };
  }

}
