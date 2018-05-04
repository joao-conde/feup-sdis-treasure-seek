package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.naming.InvalidNameException;

public abstract class Model implements Serializable {
	
	private static final long serialVersionUID = -2172782761913177196L;

	public static enum ModelType {
		USER("users","user"),
		TREASURE("treasures","treasure"),
		FOUND_TREASURE("foundTreasures","user_treasure");
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("users","treasures","foundTreasures"));
		
		public static ModelType type(String text) throws InvalidNameException {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return ModelType.USER;
				case 1:
					return ModelType.TREASURE;
				case 2:
					return ModelType.FOUND_TREASURE;
				default:
					throw new InvalidNameException("Invalid Model Type");
			
			}
					
		}
	
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
	private HashMap<String,Object> values = new HashMap<>();
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
