import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Create } from '../create/create';
import { List } from '../list/list';


@Component({
  selector: 'app-todolist',
  imports: [FormsModule, Create, List ],
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
