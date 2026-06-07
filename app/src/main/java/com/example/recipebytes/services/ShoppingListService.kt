package com.example.recipebytes.services

import com.example.recipebytes.models.ShoppingList
import com.example.recipebytes.models.ShoppingListItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object ShoppingListService {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth     = FirebaseAuth.getInstance()

    private fun shoppingRef() = auth.currentUser?.uid?.let {
        database.child("users").child(it).child("shoppingLists")
    }

    fun saveShoppingList(
        list: ShoppingList,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val ref = shoppingRef() ?: return
        val newRef = if (list.id.isEmpty()) ref.push() else ref.child(list.id)
        val id = newRef.key ?: return

        val itemsMap = mutableMapOf<String, Map<String, Any>>()
        list.items.forEachIndexed { index, item ->
            itemsMap[index.toString()] = mapOf(
                "name"        to item.name,
                "quantity"    to item.quantity,
                "isChecked"   to item.isChecked,
                "recipeTitle" to item.recipeTitle
            )
        }

        val data = mapOf(
            "id"           to id,
            "recipeTitle"  to list.recipeTitle,
            "createdAt"    to list.createdAt,
            "items"        to itemsMap
        )

        newRef.setValue(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed") }
    }

    fun listenShoppingLists(onResult: (List<ShoppingList>) -> Unit): ValueEventListener? {
        val ref = shoppingRef() ?: run { onResult(emptyList()); return null }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<ShoppingList>()
                for (child in snapshot.children) {
                    val id          = child.child("id").value as? String ?: ""
                    val recipeTitle = child.child("recipeTitle").value as? String ?: ""
                    val createdAt   = (child.child("createdAt").value as? Long) ?: 0L

                    val items = mutableListOf<ShoppingListItem>()
                    for (itemChild in child.child("items").children) {
                        items.add(ShoppingListItem(
                            name        = itemChild.child("name").value as? String ?: "",
                            quantity    = itemChild.child("quantity").value as? String ?: "",
                            isChecked   = itemChild.child("isChecked").value as? Boolean ?: false,
                            recipeTitle = itemChild.child("recipeTitle").value as? String ?: ""
                        ))
                    }
                    lists.add(ShoppingList(id, recipeTitle, createdAt, items))
                }
                onResult(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        return listener
    }

    fun updateItemChecked(listId: String, itemIndex: Int, isChecked: Boolean) {
        shoppingRef()?.child(listId)?.child("items")
            ?.child(itemIndex.toString())?.child("isChecked")?.setValue(isChecked)
    }

    fun removeListener(listener: ValueEventListener) {
        shoppingRef()?.removeEventListener(listener)
    }

    fun deleteShoppingList(listId: String) {
        shoppingRef()?.child(listId)?.removeValue()
    }
}