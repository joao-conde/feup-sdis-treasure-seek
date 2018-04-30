package model;

import java.util.Arrays;
import java.util.HashMap;

public abstract class Model {
	
	public static enum ModelType {
		User("User","user"),
		Treasure("Treasure","treasure"),
		TreasureFound("TreasureFound","user_treasure");
	
		private ModelType(String modelName, String tableName) {
			this.modelName = modelName;
			this.tableName = tableName;
		}
		
		private String modelName;
		private String tableName;
		public String getModelName() {
			return modelName;
		}
		public String getTableName() {
			return tableName;
		}
		
		
	}
	
	private ModelType modelType;
	private HashMap<String,Object> values;
	private String[] fields;
	
	public Model(ModelType modelType, String[] fields) {
		this.modelType = modelType;
		this.fields = fields;

	}
	
	public ModelType getModelType() {
		return modelType;
	}

	public String[] getFields() {
		return fields;
	}

	public void setValues(HashMap<String, Object> values) {
		this.values = values;
	}

	public HashMap<String, Object> getValues() {
		return values;
	}
	
	public boolean setValue(String field, Object value) {
		
		if(Arrays.asList(fields).contains(field)) {
			this.values.put(field, value);
			return true;
		}
		
		return false;

	}
	
	public Object getValue(String field) {
		
		if(Arrays.asList(fields).contains(field))
			return this.values.get(field);
		
		return null;
		
	}
		
}
