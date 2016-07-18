package org.zenframework.z8.server.ie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.zenframework.z8.server.base.table.Table;
import org.zenframework.z8.server.base.table.value.Field;
import org.zenframework.z8.server.db.sql.SqlToken;
import org.zenframework.z8.server.engine.RmiIO;
import org.zenframework.z8.server.engine.RmiSerializable;
import org.zenframework.z8.server.engine.Runtime;
import org.zenframework.z8.server.types.guid;

public class ExportSource implements RmiSerializable, Serializable {
	private static final long serialVersionUID = 9093748008996862263L;

	private String tableName;
	private Collection<String> fieldNames = new ArrayList<String>();
	private Collection<guid> records;

	private Table table;
	private Collection<Field> fields;
	
	public ExportSource() {
	}
	
	public ExportSource(Table table, Collection<Field> fields, Collection<guid> records) {
		this(table, fields);
		this.records = records;
	}
	
	public ExportSource(Table table, Collection<Field> fields, SqlToken where) {
		this(table, fields);

		if(where != null)
			initRecords(where);
	}
	
	private ExportSource(Table table, Collection<Field> fields) {
		this.table = table;
		this.tableName = table.name();

		this.fields = fields == null ? table.getPrimaryFields() : fields;
		
		for(Field field : this.fields) {
			if(field.exportable())
				this.fieldNames.add(field.name());
		}
	}
	
	private void initRecords(SqlToken where) {
		records = new ArrayList<guid>();
		
		Table table = table();
		
		table.saveState();
		
		table.read(Arrays.asList((Field)table.recordId.get()), where);
		while(table.next())
			records.add(table.recordId());
	
		table.restoreState();
	}
	
	public String name() {
		return tableName;
	}

	public Table table() {
		if(table == null)
			table = (Table)Runtime.instance().getTable(tableName).newInstance();
		return table;
	}

	public Collection<Field> fields() {
		if(fields != null)
			return fields;
		
		Table table = table();

		fields = new ArrayList<Field>();
		
		for(String fieldName : fieldNames) {
			Field field = table.getFieldByName(fieldName);
			if(field == null)
				throw new RuntimeException("Field not found: '" + tableName + "'.'" + fieldName + "'; schema version: " + Runtime.version());
			fields.add(field);
		}
		
		return fields;
	}
	
	public Collection<guid> records() {
		return records;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		serialize(out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		deserialize(in);
	}

	@Override
	public void serialize(ObjectOutputStream out) throws IOException {
		RmiIO.writeLong(out, serialVersionUID);

		RmiIO.writeString(out, tableName);
		out.writeObject(fieldNames);
		out.writeObject(records);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
		@SuppressWarnings("unused")
		long version = RmiIO.readLong(in);

		tableName = RmiIO.readString(in);
		fieldNames = (Collection<String>)in.readObject();
		records = (Collection<guid>)in.readObject();
	}
}
