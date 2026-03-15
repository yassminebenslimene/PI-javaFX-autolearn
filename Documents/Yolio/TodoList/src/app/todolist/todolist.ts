import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';


@Component({
  selector: 'app-todolist',
  imports: [FormsModule],
  templateUrl: './todolist.html',
  styleUrl: './todolist.css',
})
export class Todolist {

  todo = { text: ''};
  todos =  [ {
    text: 'Learn Angular'
  }];

  addtodo() {
    this.todos.push(this.todo);
    this.todo = { text: '' };
  }
}
